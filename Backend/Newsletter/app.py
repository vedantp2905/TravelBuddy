import os
import requests
from typing import Type, Any
from crewai_tools import ScrapeWebsiteTool
from pydantic.v1 import BaseModel, Field
from crewai_tools.tools.base_tool import BaseTool
from langchain_google_genai import ChatGoogleGenerativeAI
from crewai import Agent, Task, Crew, Process
from flask import Flask, jsonify, request
from dotenv import load_dotenv
from flask_cors import CORS
import traceback
import asyncio
from concurrent.futures import ThreadPoolExecutor
from contextvars import ContextVar
from jinja2 import Template
import markdown2
import re

# Add this function at the top level of your file
def run_async(func):
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    return loop.run_until_complete(func)

# Create a ContextVar to store the event loop
current_loop = ContextVar('current_loop', default=None)

app = Flask(__name__)
CORS(app)  # Add this line to enable CORS

# Update this line to use the correct path to your .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))

required_env_vars = ["GOOGLE_API_KEY", "serp_api"]
for var in required_env_vars:
    if not os.getenv(var):
        raise EnvironmentError(f"Required environment variable {var} is not set.")

class SerpApiGoogleSearchToolSchema(BaseModel):
    q: str = Field(..., description="Parameter defines the query you want to search. You can use anything that you would use in a regular Google search. e.g. inurl:, site:, intitle:.")
    tbs: str = Field("qdr:w2", description="Time filter to limit the search to the last two weeks.")

class SerpApiGoogleSearchTool(BaseTool):
    name: str = "Google Search"
    description: str = "Search the internet"
    args_schema: Type[BaseModel] = SerpApiGoogleSearchToolSchema
    search_url: str = "https://serpapi.com/search"
    
    def _run(
        self,
        q: str,
        tbs: str = "qdr:w2",
        **kwargs: Any,
    ) -> Any:
        global serp_api_key
        payload = {
            "engine": "google",
            "q": q,
            "tbs": tbs,
            "api_key": os.getenv("serp_api"),
        }
        headers = {
            'content-type': 'application/json'
        }
    
        response = requests.get(self.search_url, headers=headers, params=payload)
        results = response.json()
    
        summary = ""
        for key in ['answer_box_list', 'answer_box', 'organic_results', 'sports_results', 'knowledge_graph', 'top_stories']:
            if key in results:
                summary += str(results[key])
                break
        
        print(summary)
        
        return summary

# HTML template using Jinja2
html_template = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Newsletter</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px; }
        h1, h2, h3 { color: #2c3e50; }
        .highlight { background-color: #f1c40f; padding: 10px; margin-bottom: 20px; }
        .article { margin-bottom: 30px; }
        .why-it-matters { font-style: italic; color: #7f8c8d; }
        a { color: #3498db; }
    </style>
</head>
<body>
    {{ content | safe }}
</body>
</html>
"""

def generate_html_email(content):
    # Remove extra newlines and spaces at the beginning and end
    cleaned_content = content.strip()
    
    # Convert Markdown to HTML
    html_content = markdown2.markdown(cleaned_content, extras=['fenced-code-blocks', 'tables'])
    
    # Remove extra newlines and spaces between lines
    html_content = re.sub(r'\s*\n\s*', ' ', html_content)
    
    # Replace multiple spaces with a single space
    html_content = re.sub(r' +', ' ', html_content)
    
    # Reinsert appropriate newlines for HTML structure
    html_content = re.sub(r'(</(p|h[1-6]|ul|ol|li|blockquote)>)', r'\1\n', html_content)
    
    # Create a Jinja2 template
    template = Template(html_template)
    
    # Render the template with the newsletter content
    html_email = template.render(content=html_content)
    
    return html_email

@app.route('/generate-newsletter', methods=['POST'])
def generate_newsletter():
    data = request.json
    topic = data.get('topic')
    
    if not topic:
        return jsonify({"error": "Topic is required"}), 400

    search_tool = SerpApiGoogleSearchTool()
    scrape_tool = ScrapeWebsiteTool()

    def create_agents_and_tasks():
        # Set up the event loop for this thread
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        current_loop.set(loop)

        llm = ChatGoogleGenerativeAI(
            model="gemini-1.5-flash",
            verbose=True,
            temperature=0.6,
            google_api_key=os.getenv("GOOGLE_API_KEY")
        )

        researcher_agent = Agent(
            role='Newsletter Content Researcher',
            goal='Search for exactly 5 stories on the given topic, find 5 unique URLs containing these stories, and scrape relevant information from these URLs.',
            backstory=(
                "An experienced researcher with strong skills in web scraping, fact-finding, and "
                "analyzing recent trends to provide up-to-date information for high-quality newsletters. "
                "Always ensures to find exactly 5 stories for each newsletter."
            ),
            verbose=True,
            allow_delegation=False,
            llm=llm,
            max_iter = 5
        )

        writer_agent = Agent(
            role='Content Writer',
            goal='Write detailed, engaging, and informative summaries of the stories found by the researcher using the format specified.',
            backstory=("An experienced writer with a background in journalism and content creation. "
                        "Skilled in crafting compelling narratives and distilling complex information into "
                        "accessible formats. Adept at conducting research and synthesizing insights for engaging content."),
            verbose=True,
            allow_delegation=False,
            llm=llm,
            max_iter = 5
        )

        reviewer_agent = Agent(
            role='Content Reviewer',
            goal='Review and refine content drafts to ensure they meet high standards of quality and impact like major newsletters.',
            backstory=("A meticulous reviewer with extensive experience in editing and proofreading, "
                        "known for their keen eye for detail and commitment to maintaining the highest quality standards in published content."),
            verbose=True,
            allow_delegation=False,
            llm=llm,
            max_iter = 5
        )

        final_writer_agent = Agent(
            role='Final Content Writer',
            goal='Compile, refine, and structure all reviewed and approved content into a cohesive and engaging newsletter format. Ensure that the final product is polished, logically structured, and ready for publication, providing a seamless and informative reading experience for the audience.',
            backstory=("An accomplished writer and editor with extensive experience in journalism, content creation, and editorial management. "
                        "Known for their ability to craft compelling narratives and ensure consistency and quality across all sections of a publication. "
                        "With a keen eye for detail and a deep understanding of audience engagement, this writer excels in transforming raw content into polished, professional-grade newsletters that captivate readers and deliver clear, valuable insights."),
            verbose=True,
            allow_delegation=False,
            llm=llm,
            max_iter = 5
        )

        task_researcher = Task(
            description=(f'Research and identify exactly 5 interesting stories on the topic of {topic} and their URLs. '
                         'Scrape detailed content from all 5 relevant URLs to gather comprehensive material. '
                         'Ensure that you provide exactly 5 stories, no more and no less.'),
            agent=researcher_agent,
            expected_output=('A list of exactly 5 recent stories within the last 2 weeks with their respective website URLs. '
                            'Scraped content from all 5 URLs that can be used further by the writer.'),
            tools=[search_tool, scrape_tool]
        )

        task_writer = Task(
            description=('Write detailed summaries of all 5 stories found by the researcher. '
                         'Ensure each summary is informative, engaging, and provides clear insights into the story. '
                         'You must produce exactly 5 summaries.'),
            agent=writer_agent,
            expected_output=('Summarized content for all 5 stories, each summary being 150-200 words long, '
                             'with clear and concise information. '
                             'Links to original source found by researcher and any additional information if needed.')
        )

        task_reviewer = Task(
            description=('Review the summarized content provided by the writer for accuracy, coherence, and quality. '
                         'Ensure that the content is free from errors and meets the publication standards. '
                         'Verify that there are exactly 5 story summaries.'),
            agent=reviewer_agent,
            expected_output=('Reviewed content with suggestions for improvements, if any. '
                             'Final versions of 5 summaries that are ready for inclusion in the newsletter. '
                             'Verify the links are opening to correct pages.')
        )

        task_final_writer = Task(
            description=('Compile the reviewed and refined content into a well-structured newsletter format. '
                         'Ensure the newsletter is visually appealing and flows logically from one section to the next. '
                         'Use proper Markdown formatting for headers, lists, and links. '
                         'The newsletter must include exactly 5 stories.'),
            agent=final_writer_agent,
            expected_output=(
                """Final newsletter document with all 5 reviewed summaries, formatted in Markdown and ready for publication. 
                The newsletter should include:
                # AI Newsletter: [Topic]

                ## Contents:
                - [List all 5 stories with a brief, engaging description]

                ## Newsletter Highlight
                [Highlight the most interesting story]

                ## [Story 1 Title]
                [Small introduction]
                - [Detail point 1]
                - [Detail point 2]
                - [Detail point 3]
                
                **Why it matters:** [Explanation or call to action]
                
                [Link to original source](URL)

                [Repeat the above structure for each of the 5 stories]

                ## Conclusion
                [Wrap up the newsletter with a summary and final thought]
                """
            )
        )

        crew = Crew(
            agents=[researcher_agent, writer_agent, reviewer_agent, final_writer_agent],
            tasks=[task_researcher, task_writer, task_reviewer, task_final_writer],
            verbose=2,
            output_format="json"
        )

        result = crew.kickoff()

        # Clean up the event loop
        loop.close()
        return result

    with ThreadPoolExecutor() as executor:
        result = executor.submit(create_agents_and_tasks).result()

    # Generate HTML content
    html_content = generate_html_email(result)

    return jsonify({
        "formatted_html": html_content
    }), 200

@app.route('/test', methods=['GET'])
def test():
    return jsonify({"message": "Server is working!"}), 200

if __name__ == '__main__':
    try:
        app.run(host='0.0.0.0', port=5005, debug=True, threaded=True)
    except Exception as e:
        print(f"An error occurred: {e}")
        print(traceback.format_exc())