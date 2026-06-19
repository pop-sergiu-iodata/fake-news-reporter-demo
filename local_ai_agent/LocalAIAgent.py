import requests
import json
import os
import sys
from typing import TypedDict, List
from langgraph.graph import StateGraph

# Configuration
OLLAMA_URL = "http://localhost:11434/api/generate"
LOCAL_MODEL = "mistral"

CATEGORIES = [
    "Politics",
    "Health",
    "Science",
    "Technology",
    "Entertainment",
    "Finance",
    "Other"
]

class NewsItem(TypedDict):
    id: int
    text: str
    category: str

class BatchState(TypedDict):
    items: List[NewsItem]
    processed_count: int

def classify_single_text(text: str) -> str:
    prompt = f"""
    You are a professional news editor. 
    Classify the following news content into EXACTLY ONE of these categories:
    {", ".join(CATEGORIES)}

    Rules:
    - Respond with ONLY the category name.
    - Do not provide reasoning.
    - If unsure, respond with 'Other'.

    Content to classify:
    {text}
    """
    
    payload = {
        "model": LOCAL_MODEL,
        "prompt": prompt,
        "stream": False
    }

    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=60)
        response.raise_for_status()
        result = response.json()
        raw_category = result.get('response', 'Other').strip()
        
        for cat in CATEGORIES:
            if cat.lower() in raw_category.lower():
                return cat
    except Exception:
        pass
    return "Other"

def process_batch(state: BatchState):
    items = state["items"]
    for item in items:
        item["category"] = classify_single_text(item["text"])
    return {"items": items, "processed_count": len(items)}

# Graph setup
workflow = StateGraph(BatchState)
workflow.add_node("process", process_batch)
workflow.set_entry_point("process")
workflow.add_edge("process", "__end__")

app = workflow.compile()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps([]))
        sys.exit(0)
    
    try:
        # Read JSON input from the file path provided by Java
        input_file_path = sys.argv[1]
        with open(input_file_path, 'r', encoding='utf-8') as f:
            input_data = json.load(f)
        
        # Expected format: [{"id": 1, "text": "..."}]
        
        result = app.invoke({"items": input_data, "processed_count": 0})
        # Output JSON back to Java
        print(json.dumps(result["items"]))
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        print(json.dumps([]))
