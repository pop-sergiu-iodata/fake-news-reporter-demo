# AI News Agent - 

This directory (ai_agent) contains a Python-based AI agent that fetches and analyzes the top news articles (*and saves them).

## Prerequisites

*   Python 3.14
*   NewsAPI key
*   Gemini API key

## 1. Setup Environment Variables

To ensure security and flexibility, the agent uses environment variables for its API keys.

1.  Open *Environment Variables* in Windows (search for "Edit the system environment variables").
2.  Under *User variables for (...)*, create two new variables:
2.1.   `GEMINI_API_KEY_AGENT`: (Your Gemini API Key, not in the project because of gitBots)
2.2.   `NEWS_API_KEY_AGENT`: (Your NewsAPI Key, not in the project because of gitBots)
3.  Click *OK* on all windows to save the changes.
4.  *RESTART* or close your terminal and IDE (VS Code/IntelliJ) for the changes to take effect.

## 2. Initialize Virtual Environment

If needed, the Python Community Edition can be installed in Intellij IDEA from settings -> Plugins -> Search for "Python Community Edition".

(Already created): Create a folder/or a Python project, where the python code will exist.

Run these commands from the **project root** directory:

```powershell
# Create the virtual environment
python -m venv ai_agent/.venv

# Activate the virtual environment
.\ai_agent\.venv\Scripts\activate

# Install dependencies
pip install requests langgraph beautifulsoup4 feedparser langchain-core
```

## 3. Running the Agent manually (optional)

Run the agent from the terminal to verify it's working:

```powershell
# From the project root
.\ai_agent\.venv\Scripts\python.exe ai_agent/TestAgain.py
```

It should output a JSON array of 3 analyzed articles.

```output example
[
   {
     "article": "A Falcon 9 rocket stands poised to launch from the Space Launch Complex 4E at Vandenberg Space
      Force Base in California. File Photo: SpaceX. SpaceX launched a Falcon 9 rocket from Vandenberg Space Fo… [+937
      chars]",
     "verdict": "REAL",
     "analysis": {
       "summary": "The article mentions a Falcon 9 rocket launching from Vandenberg Space Force Base.",
       "reasoning": "This is a well-documented and recurring event. SpaceX routinely launches Falcon 9 rockets from
      this location, and the information provided is consistent with publicly available launch schedules and news
      reports. The mention of a 'File Photo' also suggests it's referencing a standard or past event, rather than a
      fabricated one."
     }
   },
   {
     "article": "Tracking data appears to show a number of Iran-linked ships passing through the Strait of Hormuz
      in the hours after the U.S. blockade of the waterway began on Monday. The U.S. military said its bloc… [+1262
      chars]",
     "verdict": "ERROR",
     "analysis": "Analysis failed: 'candidates'"
   },
   {
     "article": "US Vice President JD Vance has said that mistrust between Iran and the United States can't be
      resolved overnight. But Iranian negotiators wanted to make a deal, he said on Tuesday evening. Earlier… [+793
      chars]",
     "verdict": "REAL (With Caveat)",
     "analysis": {
       "plausibility": "The statement attributed to 'US Vice President JD Vance' is entirely plausible. It reflects
      a common sentiment regarding US-Iran relations and the complexities of negotiation.",
       "attribution": "It attributes the statement to a specific, named individual (JD Vance) and specifies the
      time ('Tuesday evening').",
       "likely_source": "This sounds like a report from a legitimate news outlet summarizing remarks made by a
      public figure.",
       "caveat": "The article is incomplete '+793 chars]'. Without the full content, it's impossible to be 100%
      certain. Confirmation would require the full article and the reputable source it's from."
        }
    }
]
```

## 4. Web application


Run docker

```powershell
docker-compose up --build
```

then decompose localhost:8080 from docker desktop

then run these commands

```powershell
mvn clean package

mvn spring-boot:run
```
