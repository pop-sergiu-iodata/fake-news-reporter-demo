import json
from pathlib import Path

from litellm import completion
from smolagents import tool

from mcp_client import (
    fetch_news_via_mcp,
    fetch_article_via_mcp,
    download_images_via_mcp,
)

from tools import (
    calculate,
    list_workspace_files,
    read_workspace_file,
    write_workspace_file,
)


WORKSPACE = Path("./workspace").resolve()
WORKSPACE.mkdir(exist_ok=True)

MODEL_ID = "ollama_chat/llama3.2:3b"


def normalize_articles(data):
    if isinstance(data, str):
        try:
            data = json.loads(data)
        except Exception:
            return []

    if isinstance(data, dict):
        if "result" in data and isinstance(data["result"], list):
            return data["result"]
        if "articles" in data and isinstance(data["articles"], list):
            return data["articles"]
        return [data]

    if isinstance(data, list):
        return [item for item in data if isinstance(item, dict)]

    return []


def save_json(relative_path: str, data) -> str:
    path = WORKSPACE / relative_path
    path.parent.mkdir(parents=True, exist_ok=True)

    path.write_text(
        json.dumps(data, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )

    return str(path)


def classify_article(title: str, description: str, article_text: str = "") -> str:
    prompt = f"""
Classify this news article into exactly one category.

Allowed categories:
Politics, Health, Science, Technology, Entertainment, Finance, Other

Title:
{title}

Description:
{description}

Article text preview:
{article_text[:1000]}

Return only the category name.
"""

    try:
        response = completion(
            model=MODEL_ID,
            api_base="http://localhost:11434",
            messages=[
                {
                    "role": "user",
                    "content": prompt,
                }
            ],
            temperature=0,
        )

        category = response["choices"][0]["message"]["content"].strip()

        allowed = {
            "Politics",
            "Health",
            "Science",
            "Technology",
            "Entertainment",
            "Finance",
            "Other",
        }

        if category not in allowed:
            return "Other"

        return category

    except Exception:
        return "Other"


def compact_articles_for_output(articles: list[dict]) -> list[dict]:
    compact = []

    for article in articles:
        compact.append(
            {
                "rank": article.get("rank", ""),
                "title": article.get("title", ""),
                "url": article.get("url", ""),
                "description": article.get("description", ""),
                "category": article.get("category", "Other"),
                "image_urls": article.get("image_urls", [])[:2],
                "local_image_path": article.get("local_image_path", ""),
            }
        )

    return compact


@tool
def fetch_news_with_images(source: str = "bbc", limit: int = 5) -> str:
    """
    Fetch real RSS news through the MCP server, download one image per article,
    classify the articles, and save the result as JSON.

    Args:
        source: News source. Options: bbc, guardian, techcrunch, ars, sciencedaily.
        limit: Number of articles to fetch, between 1 and 10.
    """
    articles = fetch_news_via_mcp(source=source, limit=limit)
    articles = normalize_articles(articles)

    if not articles:
        return "No articles were returned by the MCP server."

    articles = download_images_via_mcp(
        articles=articles,
        folder=f"images/{source}",
    )
    articles = normalize_articles(articles)

    for article in articles:
        article["category"] = classify_article(
            title=article.get("title", ""),
            description=article.get("description", ""),
            article_text=article.get("article_text", ""),
        )

    saved_path = save_json(f"news/{source}_news.json", articles)

    result = {
        "message": f"Fetched {len(articles)} articles from {source}.",
        "saved_to": saved_path,
        "articles": compact_articles_for_output(articles),
    }

    return json.dumps(result, indent=2, ensure_ascii=False)

@tool
def fetch_news_with_images(source: str = "bbc", limit: str = "5") -> str:
    """
    Fetch real RSS news through the MCP server, download one image per article,
    classify the articles, and save the result as JSON.

    Args:
        source: News source. Options: bbc, guardian, techcrunch, ars, sciencedaily.
        limit: Number of articles to fetch, for example "5".
    """
    try:
        limit_int = int(limit)
    except Exception:
        limit_int = 5

    limit_int = max(1, min(limit_int, 10))

    articles = fetch_news_via_mcp(source=source, limit=limit_int)
    articles = normalize_articles(articles)

    if not articles:
        return "No articles were returned by the MCP server."

    articles = download_images_via_mcp(
        articles=articles,
        folder=f"images/{source}",
    )
    articles = normalize_articles(articles)

    for article in articles:
        article["category"] = classify_article(
            title=article.get("title", ""),
            description=article.get("description", ""),
            article_text=article.get("article_text", ""),
        )

    saved_path = save_json(f"news/{source}_news.json", articles)

    result = {
        "message": f"Fetched {len(articles)} articles from {source}.",
        "saved_to": saved_path,
        "articles": compact_articles_for_output(articles),
    }

    return json.dumps(result, indent=2, ensure_ascii=False)

@tool
def fetch_article_with_images(url: str) -> str:
    """
    Fetch one article from a direct URL through the MCP server,
    download one image, classify it, and save the result as JSON.

    Args:
        url: Direct article URL.
    """
    article = fetch_article_via_mcp(url)
    articles = normalize_articles(article)

    if not articles:
        return "No article was returned by the MCP server."

    articles = download_images_via_mcp(
        articles=articles,
        folder="images/direct_url",
    )
    articles = normalize_articles(articles)

    for article in articles:
        article["category"] = classify_article(
            title=article.get("title", ""),
            description=article.get("description", ""),
            article_text=article.get("article_text", ""),
        )

    saved_path = save_json("news/single_article.json", articles)

    result = {
        "message": "Fetched one article from URL.",
        "saved_to": saved_path,
        "articles": compact_articles_for_output(articles),
    }

    return json.dumps(result, indent=2, ensure_ascii=False)

@tool
def calculate_expression(expression: str) -> float:
    """
    Calculate a simple arithmetic expression safely.

    Args:
        expression: Arithmetic expression, for example "12 * 5 + 3".
    """
    return calculate(expression)


@tool
def list_files_tool(relative_path: str = "") -> list[dict]:
    """
    List files in the local workspace.

    Args:
        relative_path: Folder inside workspace. Empty means workspace root.
    """
    return list_workspace_files(relative_path)


@tool
def read_file_tool(relative_path: str, max_chars: int = 5000) -> str:
    """
    Read a text file from the local workspace.

    Args:
        relative_path: File path inside workspace.
        max_chars: Maximum number of characters to read.
    """
    return read_workspace_file(relative_path, max_chars)


@tool
def write_file_tool(relative_path: str, content: str) -> str:
    """
    Write a new text file inside the local workspace.

    Args:
        relative_path: File path inside workspace.
        content: Text content to write.
    """
    return write_workspace_file(relative_path, content)