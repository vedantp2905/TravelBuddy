import os
from flask import Flask, jsonify, request
from dotenv import load_dotenv
from flask_cors import CORS
import traceback
from serpapi.google_search import GoogleSearch
import google.generativeai as genai
import json  # Add this import for handling JSON responses
import http.client  # Add this import for making HTTP requests
from datetime import datetime, timedelta  # Add this import at the top of your file
import markdown  # Add this import at the top
from IATA_finder import find_nearest_international_airports
from IATA_finder import get_coordinates

# Initialize Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS

# Load environment variables from the .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))

# Check required environment variables
required_env_vars = ["GOOGLE_API_KEY", "serp_api"]
for var in required_env_vars:
    if not os.getenv(var):
        raise EnvironmentError(f"Required environment variable {var} is not set.")

# Configure Generative AI model
genai.configure(api_key=os.environ["GOOGLE_API_KEY"])
generation_config = {
    "temperature": 0.7,
    "top_p": 0.95,
    "top_k": 64,
    "max_output_tokens": 8192,
    "response_mime_type": "text/plain",
}

model = genai.GenerativeModel(
    model_name="gemini-1.5-flash",
    generation_config=generation_config,
    system_instruction=(
        "You are an expert travel planner. You are given a destination, cities, "
        "start date, end date, and a user profile. You need to generate a detailed itinerary for the trip curated and personalized for the user."
    )
)


def get_flights(departure_id, arrival_id, journey_date, adults, children):
    """Fetch flight details using SerpAPI and return flights."""
    try:
        params = {
            "engine": "google_flights",
            "departure_id": departure_id,
            "arrival_id": arrival_id,
            "outbound_date": journey_date,
            "adults": int(adults),
            "children": int(children),
            "type": "2",  # One way
            "currency": "USD",
            "hl": "en",
            "api_key": os.environ["serp_api"]
        }

        search = GoogleSearch(params)
        results = search.get_dict()
        

        if not results or ('error' in results):
            print(results.get('error', 'Unknown error occurred'))
            return []

        def format_flight(flight):
            try:
                if not flight or 'flights' not in flight:
                    return None

                total_duration = sum(segment.get('duration', 0) for segment in flight['flights'])
                formatted = {
                    'airline': flight['flights'][0].get('airline', 'N/A'),
                    'price': flight.get('price', 'N/A'),
                    'duration': f"{total_duration} minutes",
                    'departure_airport': flight['flights'][0]['departure_airport']['name'],
                    'arrival_airport': flight['flights'][-1]['arrival_airport']['name'],
                    'departure_time': flight['flights'][0]['departure_airport']['time'],
                    'arrival_time': flight['flights'][-1]['arrival_airport']['time']
                }
                return formatted
            except Exception as e:
                print(f"Error: {str(e)}")
                print(f"Flight data: {json.dumps(flight, indent=2)}")
                return None

        # First try to get best flights
        if 'best_flights' in results and results['best_flights']:
            formatted_flights = [f for f in (format_flight(flight) for flight in results['best_flights']) if f]
            if formatted_flights:
                return formatted_flights

        # If no best flights found, try to get all available flights
        if 'flights' in results and results['flights']:
            formatted_flights = [f for f in (format_flight(flight) for flight in results['flights']) if f]
            if formatted_flights:
                return formatted_flights

        return []

    except Exception as e:
        print(f"Error: {str(e)}")
        traceback.print_exc()
        return []

def get_user_profile(userId):
    """Fetch user profile from the local endpoint API."""
    conn = http.client.HTTPConnection("localhost", 8080)
    conn.request("GET", f"/api/users/profile/{userId}")
    response = conn.getresponse()
    data = response.read()
    conn.close()
    return json.loads(data)  # Parse the JSON response

def get_username(userId):
    """Fetch user profile from the local endpoint API."""
    conn = http.client.HTTPConnection("localhost", 8080)
    conn.request("GET", f"/api/users/{userId}")
    response = conn.getresponse()
    data = response.read()
    conn.close()
    user_profile = json.loads(data)  # Parse the JSON response
    return user_profile.get('username'), user_profile.get('age'), user_profile.get('gender')  

def get_hotels(city, check_in_date, check_out_date, adults, children):
    """Fetch hotel details using SerpAPI."""
    params = {
        "engine": "google_hotels",
        "q": f"{city} Hotels",
        "check_in_date": check_in_date,
        "check_out_date": check_out_date,
        "adults": int(adults),
        "children": int(children),
        "currency": "USD",
        "gl": "us",
        "hl": "en",
        "api_key": os.environ["serp_api"]
    }
    
    search = GoogleSearch(params)
    return search.get_dict()

@app.route('/generate-itinerary', methods=['POST'])
def generate_itinerary():
    """Generate a detailed travel itinerary."""
    try:
        data = request.json
        if not data:
            return jsonify({"error": "No data provided."}), 400

        country = data.get('country')
        cities = data.get('cities', [])
        start_date = data.get('start_date')
        end_date = data.get('end_date')
        number_of_adults = data.get('number_of_adults')
        number_of_children = data.get('number_of_children')
        user_location = data.get('user_location')
        userId = data.get('userId')

        # Check for missing parameters
        if not country or not start_date or not end_date or not user_location or not userId or not number_of_adults:
            return jsonify({"error": "Missing required parameters."}), 400

        # Convert date strings to datetime objects
        try:
            start_date_obj = datetime.strptime(start_date, "%Y-%m-%d")
            end_date_obj = datetime.strptime(end_date, "%Y-%m-%d")
        except ValueError as ve:
            return jsonify({"error": f"Invalid date format: {ve}"}), 400

        current_date = datetime.now()

        # Check if the dates are in the future
        if start_date_obj <= current_date:
            return jsonify({"error": "Start date must be in the future."}), 400
        if end_date_obj <= current_date:
            return jsonify({"error": "End date must be in the future."}), 400

        # Check if the end date is after the start date
        if end_date_obj <= start_date_obj:
            return jsonify({"error": "End date must be after start date."}), 400

        total_days = (end_date_obj - start_date_obj).days

        # Fetch user profile
        try:
            user_profile = get_user_profile(userId)
            username, age, gender = get_username(userId)
        except Exception as e:
            print(f"Error fetching user profile: {e}")
            return jsonify({"error": "Failed to fetch user profile."}), 500

        # Fetch flights
        try:
            latitude, longitude = get_coordinates(user_location)
            departure_airports = find_nearest_international_airports(latitude, longitude, 1)
            if not departure_airports:
                return jsonify({"error": "No international airports found near departure location."}), 400
            
            latitude, longitude = get_coordinates(cities[0])
            arrival_airports = find_nearest_international_airports(latitude, longitude, 1)
            if not arrival_airports:
                return jsonify({"error": "No international airports found near arrival location."}), 400
                
            departure_id = departure_airports[0][1]['iata']
            arrival_id = arrival_airports[0][1]['iata']
            
            onward_flights = get_flights(
                departure_id=departure_id,
                arrival_id=arrival_id,
                journey_date=start_date, 
                adults=number_of_adults,
                children=number_of_children
            )
            if 'error' in onward_flights:
                print("Error fetching flights:", onward_flights['error'])
                return jsonify({"error": "Failed to fetch flights."}), 500
        except Exception as e:
            print(f"Error fetching flights: {e}")
            return jsonify({"error": "Failed to fetch flights."}), 500

        # Fetch return flights
        try:
            latitude, longitude = get_coordinates(cities[-1])
            departure_airports = find_nearest_international_airports(latitude, longitude, 1)
            if not departure_airports:
                return jsonify({"error": "No international airports found near departure location."}), 400
            
            return_departure_id = departure_airports[0][1]['iata']
            return_flights = get_flights(
                departure_id=return_departure_id,
                arrival_id=departure_id,
                journey_date=end_date,
                adults=number_of_adults,
                children=number_of_children
            )
            if 'error' in return_flights:
                print("Error fetching flights:", return_flights['error'])
                return jsonify({"error": "Failed to fetch flights."}), 500
        except Exception as e:
            print(f"Error fetching flights: {e}")
            return jsonify({"error": "Failed to fetch flights."}), 500

        # Fetch hotels for each city
        hotels_data = {}
        for city in cities:
            try:
                # For each city, get hotels for consecutive 2-day periods
                current_date = start_date_obj
                while current_date < end_date_obj:
                    next_date = min(current_date + timedelta(days=2), end_date_obj)
                    hotels = get_hotels(
                        city=city,
                        check_in_date=current_date.strftime("%Y-%m-%d"),
                        check_out_date=next_date.strftime("%Y-%m-%d"),
                        adults=number_of_adults,
                        children=number_of_children
                    )
                    if city not in hotels_data:
                        hotels_data[city] = []
                    hotels_data[city].append(hotels)
                    current_date = next_date
            except Exception as e:
                print(f"Error fetching hotels for {city}: {e}")

        # Generate itinerary using generative AI
        try:
            response = model.generate_content(f"""
Generate a detailed **personalized travel itinerary** for a user traveling from {user_location} to {country}. 
The user will visit the following cities: {cities}. If no cities are provided then generate a itinerary for a trip to {country} with the most travel oriented cities. The trip starts on {start_date} and ends on {end_date}, lasting {total_days} days.

---

### **User Profile:**
{user_profile}
Username: {username}
Age: {age}
Gender: {gender}

---

### **Available Flights:**
Here are the best available onward flight options:
{json.dumps(onward_flights, indent=2)}

Here are the best available return flight options:
{json.dumps(return_flights, indent=2)}

---

### **Available Hotels by City:**
{json.dumps(hotels_data, indent=2)}

### **Total number of adults:**
{number_of_adults}

### **Total number of children:**
{number_of_children}

---

### **Itinerary Guidelines:**
1. **Daily Breakdown:** Provide detailed activities for each day, labeled as "Day 1", "Day 2", etc.
2. **Balanced Itinerary:** Include a mix of adventure, relaxation, sightseeing, and local experiences based on the user's preferences.
3. **Hotel Selections:** Choose from the provided hotels list, considering:
   - Location relative to planned activities
   - User's budget and preferences
   - Available amenities
   - Guest ratings and reviews
4. **Mention the flight details for the main return jounrey from the best flights**
5. **Transport Options:** Mention public transport, car rentals, and ride-sharing options with approximate costs.
6. **Food Recommendations:** Suggest local restaurants or cafes. If the user has dietary restrictions, provide alternatives if known.
7. **Unique Experiences:** List any hidden gems, markets, festivals, or scenic viewpoints that are unique to the destination.
8. **Budget Summary:** Provide a daily breakdown of estimated costs (e.g., activities, food, transport). Include a **total estimated budget** for the entire trip.

---

### **Constraints:**
- Ensure the start and end dates are in the future.
- Align activities with the weather forecast or any local events during the trip predicted to happen during that time of year.
- Provide **backup options** for each city in case of bad weather or closures.
- Select the **best flight options** from the available flights, choosing the cheapest flights.
- Ensure the entire trip stays **within the specified budget**.

---

### **Expected Output Format:**
- **Flight Details:** Mention the flight details for the journey including onward and return flights. Include duration, price, departure and arrival airport and time. Try to stick to the airlines the user has selected in the profile. If the user has not selected any airlines, then select the best available flights from any airline.
- **Day-wise Breakdown:** Use headings like "Day 1: Arrival in {country} and city {cities[0]}".
- **Activities:** List morning, afternoon, and evening activities under each day.
- **Flight and Hotel Info:** Clearly mention any flight or hotel bookings per day if applicable. If flying between cities, give an estimate of the cost of the flight.
- **Costs:** Include a summary of expenses for the day and an updated total budget.

---

### **Budget Summary:**
Breakdown of the budget for the trip
---

### **Summary:**
- Provide a summary of the itinerary, including the total number of days, cities visited, and a brief overview of the activities and experiences planned.

Ensure that the final itinerary is **concise, well-organized**, and easy to follow. Include all the provided cities in the itinerary.
Be within the budget provided by the user including the flights, hotels and activities. If exceeding then say by how much. DO NOT EXCEED THE BUDGET.
""")

            # Convert markdown to HTML before sending
            html_content = markdown.markdown(response.text)
            
            return {"itinerary": html_content}, 200

        except Exception as e:
            print(f"Error generating itinerary: {e}")
            return jsonify({"error": "Failed to generate itinerary."}), 500

    except Exception as e:
        print(f"Unexpected error: {e}")
        print(traceback.format_exc())
        return jsonify({"error": "An unexpected error occurred."}), 500

@app.route('/test', methods=['GET'])
def test():
    """Test route to check server status."""
    return jsonify({"message": "Server is working!"}), 200

@app.route('/test-flights', methods=['GET'])
def test_flights():
    """Test route to check flight parsing."""
    try:
        flights = get_flights('DSM', 'LAX', '2024-11-01', 1, 0)  # Simplified parameters
        return jsonify(flights)
    except Exception as e:
        print(f"Error in test_flights: {str(e)}")
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    try:
        app.run(host='0.0.0.0', port=5000, debug=True, threaded=True)
    except Exception as e:
        print(f"An error occurred: {e}")
        print(traceback.format_exc())
