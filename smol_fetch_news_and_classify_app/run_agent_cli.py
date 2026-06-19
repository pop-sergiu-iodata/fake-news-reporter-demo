import sys
import json
from smolagents_app import manager_agent

def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "No prompt provided"}))
        return

    prompt = sys.argv[1]
    try:
        # manager_agent.run returns the final answer string
        result = manager_agent.run(prompt)
        print(result)
    except Exception as e:
        print(json.dumps({"error": str(e)}))

if __name__ == "__main__":
    main()
