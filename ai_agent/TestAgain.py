import requests
from bs4 import BeautifulSoup
import feedparser
import json
import os
from typing import TypedDict, List
from langgraph.graph import StateGraph

# API keys from Environment Variables
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY_AGENT")
NEWS_API_KEY = os.environ.get("NEWS_API_KEY_AGENT")

# shared memory
class NewsState(TypedDict):
    articles: List[dict]
    current_article: dict
    analysis: str
    results: List[dict]

def search_news(state: NewsState):
    if not NEWS_API_KEY:
        return {"articles": [], "results": []}
    
    url = "https://newsapi.org/v2/top-headlines"
    params = {
        "apiKey": NEWS_API_KEY,
        "country": "us",
        "pageSize": 3
    }
    
    try:
        response = requests.get(url, params=params).json()
        articles = []
        for article in response.get("articles", []):
            content = article.get("content") or article.get("description") or "No content available"
            articles.append({
                "title": article.get("title", "No Title"),
                "url": article.get("url", "N/A"),
                "content": content,
                "source": article.get("source", {}).get("name", "Unknown Source")
            })
        return {"articles": articles, "results": []}
    except Exception as e:
        return {"articles": [], "results": []}

def fetch_article(state: NewsState):
    article = state["articles"].pop(0) if state["articles"] else {"title": "N/A", "url": "N/A", "content": "", "source": "N/A"}
    return {
        "current_article": article,
        "articles": state["articles"]
    }

def analyze_article(state: NewsState):
    if not GEMINI_API_KEY:
        return {"analysis": "ERROR: GEMINI_API_KEY_AGENT not set"}
    
    article = state["current_article"]
    text = article["content"]
    
    if not text or "ERROR" in text or "failed" in text:
        return {"analysis": text if text else "No content"}
    
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key={GEMINI_API_KEY}"
    headers = {"Content-Type": "application/json"}
    data = {
        "contents": [{"parts": [{"text": f"Analyze this news article and say if it is FAKE or REAL. Give short reasoning.\n\nArticle:\n{text}"}]}]
    }
    
    try:
        response = requests.post(url, headers=headers, json=data)
        result = response.json()
        analysis_text = result["candidates"][0]["content"]["parts"][0]["text"]
    except Exception as e:
        analysis_text = f"Analysis failed: {str(e)}"
    
    return {"analysis": analysis_text}

def store_result(state: NewsState):
    article = state["current_article"]
    result = {
        "title": article["title"],
        "url": article["url"],
        "source": article["source"],
        "content": article["content"],
        "analysis": state["analysis"]
    }
    results = state.get("results", [])
    results.append(result)
    return {"results": results}

def decide_next(state: NewsState):
    return "fetch" if state["articles"] else "__end__"

# Graph setup
graph = StateGraph(NewsState)
graph.add_node("search", search_news)
graph.add_node("fetch", fetch_article)
graph.add_node("analyze", analyze_article)
graph.add_node("store", store_result)

graph.set_entry_point("search")
graph.add_edge("search", "fetch")
graph.add_edge("fetch", "analyze")
graph.add_edge("analyze", "store")
graph.add_conditional_edges("store", decide_next, {"fetch": "fetch", "__end__": "__end__"})

app = graph.compile()

if __name__ == "__main__":
    result = app.invoke({"articles": [], "results": []})
    print(json.dumps(result["results"]))
