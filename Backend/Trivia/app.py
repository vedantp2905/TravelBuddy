from flask import Flask, jsonify
import os
import google.generativeai as genai
from flask_cors import CORS
import json
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
CORS(app)

# Configure Gemini API
genai.configure(api_key=os.environ["GEMINI_API_KEY"])

# Store chat history
previous_questions = set()

# Create the model with Google's recommended configuration
generation_config = {
    "temperature": 1.5,
    "top_p": 0.95,
    "top_k": 40,
    "max_output_tokens": 8192,
}

model = genai.GenerativeModel(
    model_name="gemini-1.5-flash",
    generation_config=generation_config,
)

# Initialize chat
chat = model.start_chat(history=[])

@app.route('/api/trivia/questions', methods=['GET'])
def generate_questions():
    try:
        example_format = '''{
            "Question": "question text",
            "A": "option 1",
            "B": "option 2",
            "C": "option 3",
            "D": "option 4",
            "Answer": "A, B, C, or D",
            "Explanation": "brief explanation"
        }'''

        prompt = f"""Generate 10 unique and diverse travel trivia questions as a JSON array. 
        Previously used questions: {list(previous_questions)}
        DO NOT repeat any of these questions or generate similar variations.
        
        Include one question from each of these categories:
        - World landmarks and monuments
        - Cultural traditions and festivals
        - Geography and natural wonders
        - Transportation and travel history
        - Local cuisines and food traditions
        - Languages and communication
        - Famous travelers and explorers
        - Travel technology and innovation
        - Climate and weather patterns
        - Travel records and unique destinations

        At least 80% of the questions should be new and unique, not appearing in the provided history. Return the response in this exact JSON format:
        [{example_format}]
        
        Make sure each question is unique and interesting."""

        print("Sending prompt to Gemini...")
        response = chat.send_message(prompt)
        response_text = response.text.strip()
        
        # Remove any "```json" markers if present
        if response_text.startswith('```json'):
            response_text = response_text[7:]
        if response_text.endswith('```'):
            response_text = response_text[:-3]
            
        response_text = response_text.strip()
        print("Cleaned response:", response_text)
        
        # Parse the JSON array from the cleaned response
        raw_questions = json.loads(response_text)
        formatted_questions = []
        
        # Count for new questions
        new_questions_count = 0
        
        for q in raw_questions:
            # Store questions to prevent repetition
            question_text = q['Question'].lower().strip()
            is_new_question = question_text not in previous_questions
            
            if is_new_question:
                new_questions_count += 1
            
            # Get the correct answer text based on the letter
            answer_letter = q['Answer']
            correct_answer = q[answer_letter]
            
            formatted_question = {
                'question': q['Question'],
                'options': [q['A'], q['B'], q['C'], q['D']],
                'correctAnswer': correct_answer,
                'explanation': q['Explanation'],
                'timeLimit': 15,
                'isNew': is_new_question  # Add metadata for debugging
            }
            formatted_questions.append(formatted_question)
            
            if is_new_question:
                previous_questions.add(question_text)
        
        # Ensure at least 80% of questions are new
        if new_questions_count < 0.8 * len(formatted_questions):
            return jsonify({"error": "Failed to generate at least 80% new questions"}), 500
        
        print("Parsed questions:", json.dumps(formatted_questions, indent=2))
        
        return jsonify({"questions": formatted_questions})

    except json.JSONDecodeError as e:
        print(f"JSON parsing error: {e}")
        print(f"Raw response that caused error: {response.text}")
        return jsonify({"error": f"Invalid JSON format: {str(e)}"}), 500
    except Exception as e:
        print(f"Error generating questions: {e}")
        print(f"Raw response that caused error: {response.text if 'response' in locals() else 'No response generated'}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)
