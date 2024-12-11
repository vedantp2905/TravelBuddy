import requests
import json

def test_trivia_service():
    try:
        print("Testing trivia service...")
        response = requests.get('http://localhost:5001/api/trivia/questions')
        print(f"Status Code: {response.status_code}")
        print(f"Response Headers: {response.headers}")
        
        if response.status_code == 200:
            data = response.json()
            questions = data.get('questions', [])
            print(f"\nReceived {len(questions)} questions")
            
            if questions:
                print("\nFirst Question Details:")
                print(json.dumps(questions[0], indent=2))
            else:
                print("No questions received!")
        else:
            print("Error Response:", response.text)
            
    except Exception as e:
        print(f"Test Error: {e}")
        print(f"Error Type: {type(e)}")
        import traceback
        print(traceback.format_exc())

if __name__ == "__main__":
    test_trivia_service() 