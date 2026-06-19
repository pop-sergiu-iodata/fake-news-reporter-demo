from smolagents import LiteLLMModel, ToolCallingAgent, tool

from smol_mcp_tools import (
    fetch_news_with_images,
    fetch_article_with_images,
    calculate_expression,
    list_files_tool,
    read_file_tool,
    write_file_tool,
)


MODEL_ID = "ollama_chat/llama3.2:3b"


model = LiteLLMModel(
    model_id=MODEL_ID,
    api_base="http://localhost:11434",
)


news_agent = ToolCallingAgent(
    tools=[
        fetch_news_with_images,
        fetch_article_with_images,
    ],
    model=model,
    name="news_agent",
    description=(
        "News specialist. It fetches real news through an MCP server, extracts article "
        "metadata, downloads one image per article, classifies the articles, and saves "
        "the result as JSON. Use it for news, RSS, BBC, Guardian, TechCrunch, Ars, "
        "ScienceDaily, article URLs, images, and classification. "
        "When calling fetch_news_with_images, pass limit as a string, for example limit='5'. "
        "Never invent news. If the tool fails, report the failure."
    ),
    max_steps=4,
)


file_agent = ToolCallingAgent(
    tools=[
        list_files_tool,
        read_file_tool,
        write_file_tool,
    ],
    model=model,
    name="file_agent",
    description=(
        "Filesystem specialist. It lists files, reads files, and writes text files "
        "inside the local workspace."
    ),
    max_steps=4,
)


math_agent = ToolCallingAgent(
    tools=[
        calculate_expression,
    ],
    model=model,
    name="math_agent",
    description=(
        "Math specialist. It only calculates arithmetic expressions."
    ),
    max_steps=3,
)


@tool
def ask_news_agent(task: str) -> str:
    """
    Send a task to the news specialist agent.

    Args:
        task: Natural-language news task, for example "get 5 BBC news with images".
    """
    return str(news_agent.run(task))


@tool
def ask_file_agent(task: str) -> str:
    """
    Send a task to the file specialist agent.

    Args:
        task: Natural-language file task, for example "list workspace files".
    """
    return str(file_agent.run(task))


@tool
def ask_math_agent(task: str) -> str:
    """
    Send a task to the math specialist agent.

    Args:
        task: Natural-language math task, for example "calculate 12 * 5 + 3".
    """
    return str(math_agent.run(task))


manager_agent = ToolCallingAgent(
    tools=[
        ask_news_agent,
        ask_file_agent,
        ask_math_agent,
    ],
    model=model,
    name="manager_agent",
    description=(
        "Manager agent. It receives the user request and delegates it to exactly one "
        "specialist agent. Use ask_news_agent for news, articles, images, categories, "
        "and RSS. Use ask_file_agent for local workspace files. Use ask_math_agent "
        "for arithmetic. Do not invent news. Real news must come from the news agent."
    ),
    max_steps=5,
)


def main():
    print("Smolagents + MCP News App")
    print("Framework: smolagents")
    print("Model: llama3.2:3b through Ollama")
    print("MCP server: news_mcp_server.py")
    print()
    print("Examples:")
    print("- get 5 BBC news with images")
    print("- fetch 3 techcrunch articles with images")
    print("- get 4 sciencedaily news and classify them")
    print("- extract this article with images: https://www.bbc.com/news")
    print("- calculate 12 * 5 + 3")
    print("- list workspace files")
    print("- exit")

    while True:
        prompt = input("\n> ").strip()

        if prompt.lower() in {"exit", "quit"}:
            break

        if not prompt:
            continue

        result = manager_agent.run(prompt)

        print("\n=== FINAL ANSWER ===")
        print(result)


if __name__ == "__main__":
    main()