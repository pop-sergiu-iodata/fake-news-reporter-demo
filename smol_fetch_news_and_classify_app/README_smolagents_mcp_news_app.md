# Smolagents + MCP News Extraction and Classification App

This project is a local AI news application that combines the **smolagents** agent framework with a custom **MCP server**.

The app can understand requests such as:

```text
get 5 BBC news with images
```

It then fetches real news articles, extracts metadata and image URLs, downloads one image per article, classifies the articles, and saves the results locally.

---

## 1. Main Technologies Used

| Component | Purpose |
|---|---|
| **smolagents** | Agent framework used to create the manager agent and specialist agents |
| **Ollama** | Runs the local LLM on the machine |
| **llama3.2:3b** | Local language model used by the agents |
| **LiteLLM** | Adapter used by smolagents to communicate with Ollama |
| **MCP Python SDK / FastMCP** | Used to create the custom MCP server |
| **MCP stdio transport** | Communication method between the MCP client and MCP server |
| **feedparser** | Reads RSS feeds such as BBC, Guardian, TechCrunch, Ars Technica, and ScienceDaily |
| **httpx** | Fetches webpages and downloads images |
| **trafilatura** | Extracts readable article text from webpages |
| **BeautifulSoup** | Extracts metadata and image URLs from article HTML |
| **Python pathlib / json / ast** | Local file handling, JSON reports, and safe math evaluation |

---

## 2. Project Structure

```text
smol_fetch_news_and_classify_app/
│
├── smolagents_app.py
│   └── Main application. Defines the smolagents manager, news agent, file agent, and math agent.
│
├── smol_mcp_tools.py
│   └── High-level smolagents tools. These tools call the MCP client and perform classification/saving.
│
├── mcp_client.py
│   └── MCP client. Starts the MCP server and calls MCP tools over stdio.
│
├── news_mcp_server.py
│   └── Custom MCP server. Exposes news extraction, image download, and workspace tools.
│
├── tools.py
│   └── Local Python tools for math and workspace file operations.
│
└── workspace/
    ├── news/
    │   └── Saved JSON / Markdown news reports.
    │
    └── images/
        └── Downloaded article images.
```

---

## 3. High-Level Architecture

```text
User Prompt
   │
   │  Example: "get 5 BBC news with images"
   ▼
Smolagents Manager Agent
   │
   │  Decides which specialist should handle the task
   ▼
News Agent / File Agent / Math Agent
   │
   │
   ├── News Agent
   │      │
   │      ▼
   │   smolagents tools
   │      │
   │      ▼
   │   MCP Client
   │      │
   │      ▼
   │   MCP Protocol over stdio
   │      │
   │      ▼
   │   Custom MCP Server
   │      │
   │      ├── fetch_rss_news()
   │      ├── fetch_article_from_url()
   │      ├── download_one_image_per_article()
   │      ├── save_news_json()
   │      ├── read_file()
   │      └── list_files()
   │
   ├── File Agent
   │      ├── list_files_tool()
   │      ├── read_file_tool()
   │      └── write_file_tool()
   │
   └── Math Agent
          └── calculate_expression()
```

---

## 4. Agent Architecture

### 4.1 Manager Agent

Defined in:

```text
smolagents_app.py
```

The manager agent receives the user request and delegates it to the correct specialist.

| User request | Delegated to |
|---|---|
| `get 5 BBC news with images` | News Agent |
| `extract this article with images: <url>` | News Agent |
| `list workspace files` | File Agent |
| `calculate 12 * 5 + 3` | Math Agent |

The manager does not directly fetch news or manipulate files. It only routes the task.

---

### 4.2 News Agent

Defined in:

```text
smolagents_app.py
```

The News Agent handles tasks related to:

```text
news fetching
RSS feeds
article URLs
image extraction
image downloading
classification
saving news reports
```

It uses high-level smolagents tools from:

```text
smol_mcp_tools.py
```

Main tools:

```text
fetch_news_with_images()
fetch_article_with_images()
```

These tools internally call the MCP client, so the news logic is executed through the MCP protocol.

---

### 4.3 File Agent

Defined in:

```text
smolagents_app.py
```

The File Agent handles workspace operations.

Tools:

```text
list_files_tool()
read_file_tool()
write_file_tool()
```

These tools are wrappers around local Python file tools from `tools.py`.

---

### 4.4 Math Agent

Defined in:

```text
smolagents_app.py
```

The Math Agent handles arithmetic requests.

Tool:

```text
calculate_expression()
```

This tool calls the safe `calculate()` function from `tools.py`.

The math tool uses Python's `ast` module instead of unsafe `eval()`.

---

## 5. MCP Architecture

The MCP part of the application consists of two files:

```text
mcp_client.py
news_mcp_server.py
```

### 5.1 MCP Client

File:

```text
mcp_client.py
```

The MCP client starts the MCP server as a subprocess and communicates with it using the MCP stdio transport.

It provides wrapper functions such as:

```text
fetch_news_via_mcp()
fetch_article_via_mcp()
download_images_via_mcp()
call_mcp_tool()
```

Example flow:

```text
fetch_news_with_images()
   ↓
fetch_news_via_mcp()
   ↓
call_mcp_tool("fetch_rss_news", {"source": "bbc", "limit": 5})
   ↓
MCP server executes fetch_rss_news()
```

---

### 5.2 MCP Server

File:

```text
news_mcp_server.py
```

The MCP server exposes tools using FastMCP:

```python
@mcp.tool()
def fetch_rss_news(...):
    ...
```

A function becomes an MCP tool when it is registered with `@mcp.tool()` inside the MCP server.

The MCP server ends with:

```python
if __name__ == "__main__":
    mcp.run(transport="stdio")
```

This makes the file run as an MCP stdio server.

---

## 6. MCP Tools Used

### `fetch_rss_news(source, limit)`

Fetches articles from an RSS source.

Supported sources:

```text
bbc
guardian
techcrunch
ars
sciencedaily
```

For each article, it extracts:

```text
rank
source
title
url
description
published date
article text
image URLs
```

Libraries used internally:

```text
feedparser      → reads the RSS feed
trafilatura     → extracts readable article text
BeautifulSoup   → extracts image URLs from HTML
httpx           → fetches webpages
```

---

### `fetch_article_from_url(url)`

Fetches and extracts a single article from a direct URL.

It returns:

```text
title
url
description
article text
image URLs
```

This is used when the user gives a specific article link instead of an RSS source.

---

### `download_one_image_per_article(articles, folder)`

Downloads one image for each article.

It:

```text
1. Reads the image_urls list from each article.
2. Skips placeholder images when possible.
3. Downloads the first usable image.
4. Saves the image in workspace/images/.
5. Adds local_image_path to the article dictionary.
```

Example output field:

```json
{
  "local_image_path": "images/bbc/1_article_title.jpg"
}
```

---

### `save_news_json(relative_path, articles)`

Saves article data as JSON inside the workspace.

Example path:

```text
workspace/news/bbc_news.json
```

This is an MCP tool because it is exposed by the MCP server, even though it works with local files.

---

### `read_file(relative_path)`

Reads a text file from the MCP server workspace.

This is also an MCP tool because it is exposed through `@mcp.tool()`.

---

### `list_files(relative_path)`

Lists files and folders inside the MCP server workspace.

This is useful for checking saved reports and downloaded images.

---

## 7. Smolagents Tools

The smolagents tools are defined in:

```text
smol_mcp_tools.py
```

These are not the same thing as MCP tools. They are tools exposed to the smolagents agents.

### `fetch_news_with_images(source, limit)`

High-level smolagents tool used by the News Agent.

It performs the full news workflow:

```text
1. Calls fetch_rss_news through MCP.
2. Normalizes the returned articles.
3. Calls download_one_image_per_article through MCP.
4. Classifies each article.
5. Saves the result as JSON.
6. Returns a compact summary to the agent.
```

---

### `fetch_article_with_images(url)`

High-level smolagents tool for direct article URLs.

Workflow:

```text
1. Calls fetch_article_from_url through MCP.
2. Downloads one image through MCP.
3. Classifies the article.
4. Saves the result locally.
```

---

### `calculate_expression(expression)`

Smolagents math tool.

It calls the local `calculate()` function from `tools.py`.

---

### `list_files_tool(relative_path)`

Lists workspace files.

---

### `read_file_tool(relative_path, max_chars)`

Reads a workspace file.

---

### `write_file_tool(relative_path, content)`

Creates a new workspace text file.

---

## 8. Example Workflow: “get 5 BBC news with images”

User prompt:

```text
get 5 BBC news with images
```

Execution flow:

```text
1. User enters prompt in smolagents_app.py.
2. manager_agent receives the prompt.
3. manager_agent delegates the task to news_agent using ask_news_agent().
4. news_agent calls fetch_news_with_images(source="bbc", limit="5").
5. fetch_news_with_images() calls fetch_news_via_mcp().
6. mcp_client.py sends a request to the MCP server.
7. news_mcp_server.py runs fetch_rss_news(source="bbc", limit=5).
8. The MCP server returns 5 BBC articles with title, URL, description, text, and image URLs.
9. fetch_news_with_images() calls download_images_via_mcp().
10. The MCP server runs download_one_image_per_article().
11. One image is saved for each article in workspace/images/bbc/.
12. Each article is classified by the local model.
13. The final article list is saved in workspace/news/bbc_news.json.
14. The agent returns a summary to the user.
```

Output files:

```text
workspace/news/bbc_news.json
workspace/images/bbc/
```

---

## 9. Framework Used

This project uses:

```text
smolagents
```

as the agent framework.

It does not use:

```text
LangGraph
CrewAI
AutoGen
```

The full stack is:

```text
smolagents        → agent framework
Ollama            → local model runtime
llama3.2:3b       → local LLM
LiteLLM           → model adapter
MCP / FastMCP     → tool server protocol
Python tools      → deterministic execution
```

---

## 10. Why MCP Is Useful Here

MCP separates the agent from the tool implementation.

Instead of the agent directly scraping websites, the app works like this:

```text
Agent
 ↓
smolagents tool
 ↓
MCP client
 ↓
MCP protocol
 ↓
MCP server
 ↓
real Python extraction tool
```

This makes the tools reusable. Any MCP-compatible client could call the same server tools.

---

## 11. Important Notes

### Image extraction

The app extracts and downloads one image per article.

Some articles may have no valid image because:

```text
the site has no article image
the image is a placeholder
the image URL is blocked
the site requires JavaScript
```

In those cases, `local_image_path` may be empty.

---

### Local model limitations

Small local models such as `llama3.2:3b` can sometimes:

```text
pass numbers as strings
produce malformed tool calls
retry the same failed call
hallucinate if a tool fails
```

To reduce this, the app uses high-level tools and tolerant argument parsing where possible.

---

### JSON reports

The final results are saved as JSON so they can be reused later for:

```text
classification
search
fake-news risk analysis
database storage
UI display
```

---

## 12. Installation

Initialize the project:

```powershell
uv init
```

Install dependencies:

```powershell
uv add smolagents litellm "smolagents[mcp]" mcp mcpadapt feedparser httpx trafilatura beautifulsoup4
```

Pull the local model:

```powershell
ollama pull llama3.2:3b
```

Make sure Ollama is running:

```powershell
ollama serve
```

Run the app:

```powershell
uv run python smolagents_app.py
```

---

## 13. Example Prompts

```text
get 5 BBC news with images
```

```text
fetch 3 techcrunch articles with images
```

```text
get 4 sciencedaily news and classify them
```

```text
extract this article with images: https://www.bbc.com/news
```

```text
calculate 12 * 5 + 3
```

```text
list workspace files
```

---

## 14. Summary

This project demonstrates a hybrid local AI application using both an agent framework and the MCP protocol.

The main contributions are:

```text
- a smolagents manager agent
- specialist news, file, and math agents
- a custom MCP server for news extraction
- MCP tools for RSS fetching, article extraction, image downloading, and file access
- local model execution through Ollama
- saved JSON reports and downloaded article images
```

The result is a local news extraction and classification assistant that can fetch real articles, download images, classify content, and store results in a local workspace.
