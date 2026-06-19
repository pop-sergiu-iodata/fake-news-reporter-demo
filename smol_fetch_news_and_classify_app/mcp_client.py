import asyncio
import json
from pathlib import Path

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


SERVER_FILE = Path(__file__).parent / "news_mcp_server.py"


server_params = StdioServerParameters(
    command="uv",
    args=[
        "run",
        "python",
        str(SERVER_FILE),
    ],
)


def _unwrap(value):
    """
    Unwrap common FastMCP result shapes.
    """
    if isinstance(value, dict):
        if "result" in value and len(value) == 1:
            return value["result"]
        if "articles" in value and len(value) == 1:
            return value["articles"]

    return value


def extract_mcp_content(result):
    """
    Convert MCP tool result into normal Python data.
    Handles:
    - structuredContent
    - structured_content
    - one text result
    - multiple text results
    """

    structured = getattr(result, "structuredContent", None)
    if structured is None:
        structured = getattr(result, "structured_content", None)

    if structured is not None:
        return _unwrap(structured)

    if not result.content:
        return None

    parsed_items = []

    for item in result.content:
        if hasattr(item, "text"):
            text = item.text.strip()

            try:
                parsed_items.append(json.loads(text))
            except Exception:
                parsed_items.append(text)
        else:
            parsed_items.append(item)

    if len(parsed_items) == 1:
        return _unwrap(parsed_items[0])

    return [_unwrap(item) for item in parsed_items]


async def call_mcp_tool_async(tool_name: str, arguments: dict):
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            result = await session.call_tool(
                tool_name,
                arguments=arguments,
            )

            return extract_mcp_content(result)


def call_mcp_tool(tool_name: str, arguments: dict):
    return asyncio.run(call_mcp_tool_async(tool_name, arguments))


def fetch_news_via_mcp(source: str = "bbc", limit=5):
    try:
        limit = int(limit)
    except Exception:
        limit = 5

    print(f"[MCP CLIENT] Calling MCP tool: fetch_rss_news(source={source}, limit={limit})")

    result = call_mcp_tool(
        "fetch_rss_news",
        {
            "source": source,
            "limit": limit,
        },
    )

    print(f"[DEBUG] fetch_news_via_mcp returned: {type(result)}")

    return result


def fetch_article_via_mcp(url: str):
    print(f"[MCP CLIENT] Calling MCP tool: fetch_article_from_url(url={url})")

    return call_mcp_tool(
        "fetch_article_from_url",
        {
            "url": url,
        },
    )


def download_images_via_mcp(articles, folder: str = "images"):
    print(f"[MCP CLIENT] Calling MCP tool: download_one_image_per_article(folder={folder})")

    result = call_mcp_tool(
        "download_one_image_per_article",
        {
            "articles": articles,
            "folder": folder,
        },
    )

    print(f"[DEBUG] download_images_via_mcp returned: {type(result)}")

    return result