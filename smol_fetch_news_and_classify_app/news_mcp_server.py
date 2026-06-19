import json
import re
import sys
from pathlib import Path
from typing import Any
from urllib.parse import urljoin, urlparse

import feedparser
import httpx
import trafilatura
from bs4 import BeautifulSoup
from mcp.server.fastmcp import FastMCP


mcp = FastMCP("news-analysis-server")

WORKSPACE = Path("./workspace").resolve()
WORKSPACE.mkdir(exist_ok=True)


DEFAULT_FEEDS = {
    "bbc": "https://feeds.bbci.co.uk/news/rss.xml",
    "guardian": "https://www.theguardian.com/world/rss",
    "techcrunch": "https://techcrunch.com/feed/",
    "ars": "https://feeds.arstechnica.com/arstechnica/index",
    "sciencedaily": "https://www.sciencedaily.com/rss/top.xml",
}


def safe_path(relative_path: str) -> Path:
    path = (WORKSPACE / relative_path).resolve()

    if not path.is_relative_to(WORKSPACE):
        raise ValueError("Path outside workspace is not allowed")

    return path


def clean_filename(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9]+", "_", text)
    text = text.strip("_")
    return text[:80] or "image"


def extract_article_text(url: str, max_chars: int = 2500) -> str:
    if not url:
        return ""

    try:
        downloaded = trafilatura.fetch_url(url)

        if not downloaded:
            return ""

        text = trafilatura.extract(downloaded)

        if not text:
            return ""

        return text.strip()[:max_chars]

    except Exception:
        return ""


def extract_article_images(url: str, max_images: int = 5) -> list[str]:
    if not url:
        return []

    try:
        with httpx.Client(timeout=15, follow_redirects=True) as client:
            response = client.get(
                url,
                headers={"User-Agent": "Mozilla/5.0"},
            )
            response.raise_for_status()
            html = response.text

        soup = BeautifulSoup(html, "html.parser")

        images = []

        og_image = soup.find("meta", property="og:image")
        if og_image and og_image.get("content"):
            images.append(urljoin(url, og_image["content"]))

        # Some sites still use this metadata name.
        # We are only extracting the image URL from the article page.
        twitter_image = soup.find("meta", attrs={"name": "twitter:image"})
        if twitter_image and twitter_image.get("content"):
            image_url = urljoin(url, twitter_image["content"])
            if image_url not in images:
                images.append(image_url)

        for img in soup.find_all("img"):
            src = img.get("src")

            if not src:
                continue

            full_url = urljoin(url, src)

            if full_url not in images:
                images.append(full_url)

            if len(images) >= max_images:
                break

        return images[:max_images]

    except Exception:
        return []


def normalize_articles_input(articles: Any) -> list[dict]:
    """
    Accept list/dict/string forms so the MCP tool does not fail validation.
    """
    if isinstance(articles, str):
        try:
            articles = json.loads(articles)
        except Exception:
            return []

    if isinstance(articles, dict):
        if "result" in articles and isinstance(articles["result"], list):
            articles = articles["result"]
        elif "articles" in articles and isinstance(articles["articles"], list):
            articles = articles["articles"]
        else:
            articles = [articles]

    if not isinstance(articles, list):
        return []

    return [item for item in articles if isinstance(item, dict)]


def choose_downloadable_image(image_urls: list[str]) -> str:
    """
    Pick a decent image URL, skipping obvious placeholders.
    """
    for image_url in image_urls:
        lowered = image_url.lower()

        if not image_url.startswith(("http://", "https://")):
            continue

        if "grey-placeholder" in lowered:
            continue

        if "placeholder" in lowered:
            continue

        if lowered.endswith(".svg"):
            continue

        return image_url

    return ""


@mcp.tool()
def fetch_rss_news(source: str = "bbc", limit: int = 5) -> list[dict]:
    """
    Fetch news from an RSS feed and return title, URL, description,
    extracted article text, and image URLs.

    Args:
        source: RSS source name. Options: bbc, guardian, techcrunch, ars, sciencedaily.
        limit: Number of articles to fetch. Must be between 1 and 20.
    """
    if limit < 1 or limit > 20:
        raise ValueError("limit must be between 1 and 20")

    if source not in DEFAULT_FEEDS:
        raise ValueError(
            f"Unknown source '{source}'. Available: {list(DEFAULT_FEEDS.keys())}"
        )

    feed_url = DEFAULT_FEEDS[source]
    feed = feedparser.parse(feed_url)

    articles = []

    for index, entry in enumerate(feed.entries[:limit], start=1):
        url = entry.get("link", "")
        title = entry.get("title", "")
        description = entry.get("summary", "") or entry.get("description", "")
        published = entry.get("published", "")

        article_text = extract_article_text(url)
        image_urls = extract_article_images(url)

        articles.append(
            {
                "rank": index,
                "source": source,
                "title": title,
                "url": url,
                "description": description,
                "published": published,
                "article_text": article_text or "Article text could not be extracted.",
                "image_urls": image_urls,
            }
        )

    return articles


@mcp.tool()
def fetch_article_from_url(url: str) -> dict:
    """
    Fetch a single article from a direct URL and extract readable text and images.

    Args:
        url: Article URL.
    """
    article_text = extract_article_text(url)
    image_urls = extract_article_images(url)

    title = ""
    description = ""

    try:
        with httpx.Client(timeout=15, follow_redirects=True) as client:
            response = client.get(
                url,
                headers={"User-Agent": "Mozilla/5.0"},
            )
            response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")

        if soup.title and soup.title.text:
            title = soup.title.text.strip()

        og_title = soup.find("meta", property="og:title")
        if og_title and og_title.get("content"):
            title = og_title["content"].strip()

        og_description = soup.find("meta", property="og:description")
        if og_description and og_description.get("content"):
            description = og_description["content"].strip()

        meta_description = soup.find("meta", attrs={"name": "description"})
        if not description and meta_description and meta_description.get("content"):
            description = meta_description["content"].strip()

    except Exception:
        pass

    return {
        "title": title,
        "url": url,
        "description": description,
        "article_text": article_text or "Article text could not be extracted.",
        "image_urls": image_urls,
    }


@mcp.tool()
def download_one_image_per_article(articles: Any, folder: str = "images") -> list[dict]:
    """
    Download one image from each article and add local_image_path to each article.

    Args:
        articles: Article dictionary, list of article dictionaries, or JSON string.
        folder: Folder inside workspace where images will be saved.

    Returns:
        The updated list of article dictionaries.
    """
    articles_list = normalize_articles_input(articles)

    output_folder = safe_path(folder)
    output_folder.mkdir(parents=True, exist_ok=True)

    updated_articles = []

    for index, article in enumerate(articles_list, start=1):
        article_copy = dict(article)
        image_urls = article_copy.get("image_urls", [])

        article_copy["local_image_path"] = ""

        if not image_urls:
            updated_articles.append(article_copy)
            continue

        image_url = choose_downloadable_image(image_urls)

        if not image_url:
            updated_articles.append(article_copy)
            continue

        try:
            with httpx.Client(timeout=20, follow_redirects=True) as client:
                response = client.get(
                    image_url,
                    headers={"User-Agent": "Mozilla/5.0"},
                )
                response.raise_for_status()

            content_type = response.headers.get("content-type", "").lower()

            if "image" not in content_type:
                updated_articles.append(article_copy)
                continue

            parsed_url = urlparse(image_url)
            extension = Path(parsed_url.path).suffix.lower()

            if extension not in {".jpg", ".jpeg", ".png", ".webp", ".gif"}:
                if "jpeg" in content_type or "jpg" in content_type:
                    extension = ".jpg"
                elif "png" in content_type:
                    extension = ".png"
                elif "webp" in content_type:
                    extension = ".webp"
                elif "gif" in content_type:
                    extension = ".gif"
                else:
                    extension = ".jpg"

            title = article_copy.get("title", f"article_{index}")
            filename = f"{index}_{clean_filename(title)}{extension}"
            image_path = output_folder / filename

            image_path.write_bytes(response.content)

            article_copy["local_image_path"] = str(image_path.relative_to(WORKSPACE))
            article_copy["downloaded_image_url"] = image_url

        except Exception as e:
            print(
                f"[MCP SERVER] Could not download image: {image_url} | {e}",
                file=sys.stderr,
            )

        updated_articles.append(article_copy)

    return updated_articles


@mcp.tool()
def save_news_json(relative_path: str, articles: Any) -> str:
    """
    Save news articles as a JSON file inside the workspace.

    Args:
        relative_path: File path inside workspace, for example "news/bbc.json".
        articles: Article data to save.
    """
    path = safe_path(relative_path)

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(articles, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )

    return f"Created {relative_path}"


@mcp.tool()
def save_file(relative_path: str, content: str) -> str:
    """
    Save a text file inside the workspace.

    Args:
        relative_path: File path inside workspace.
        content: Text content to save.
    """
    path = safe_path(relative_path)

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")

    return f"Created {relative_path}"


@mcp.tool()
def read_file(relative_path: str, max_chars: int = 5000) -> str:
    """
    Read a text file from the workspace.

    Args:
        relative_path: File path inside workspace.
        max_chars: Maximum number of characters to return.
    """
    path = safe_path(relative_path)

    if not path.exists():
        raise FileNotFoundError(relative_path)

    return path.read_text(encoding="utf-8")[:max_chars]


@mcp.tool()
def list_files(relative_path: str = "") -> list[dict]:
    """
    List files and folders inside the workspace.

    Args:
        relative_path: Folder path inside workspace. Empty means workspace root.
    """
    path = safe_path(relative_path)

    if not path.exists():
        return []

    if not path.is_dir():
        raise NotADirectoryError(relative_path)

    return [
        {
            "name": item.name,
            "type": "directory" if item.is_dir() else "file",
            "size_bytes": item.stat().st_size,
        }
        for item in path.iterdir()
    ]


if __name__ == "__main__":
    mcp.run(transport="stdio")