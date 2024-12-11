from opencage.geocoder import OpenCageGeocode
import airportsdata
from math import radians, cos, sin, asin, sqrt

def get_coordinates(city_name):
    """Gets the latitude and longitude of a given city."""
    geocoder = OpenCageGeocode('84f2a8f8882f40bc9edab0d33d0c343f')  # Replace with your API key
    results = geocoder.geocode(city_name)

    if results and len(results):
        location = results[0]['geometry']
        return location['lat'], location['lng']
    else:
        print(f"Could not find coordinates for {city_name}.")
        return None, None

def find_nearest_international_airports(latitude, longitude, num_airports=1):
    """Finds the nearest international airports to the given coordinates."""
    airports = airportsdata.load()
    
    # Store (distance, airport_data) tuples
    airport_distances = []

    # Keywords that indicate municipal/regional airports
    included_keywords = ['international']

    for airport_code, airport_data in airports.items():
        # Skip if no IATA code
        if not airport_data['iata']:
            continue
        
        if not any(keyword.lower() in airport_data['name'].lower() for keyword in included_keywords):
            continue

        airport_lat = airport_data['lat']
        airport_lon = airport_data['lon']
        distance = haversine(latitude, longitude, airport_lat, airport_lon)
        airport_distances.append((distance, airport_data))

    # Sort airports by distance and return top num_airports
    airport_distances.sort(key=lambda x: x[0])
    return airport_distances[:num_airports]

def haversine(lat1, lon1, lat2, lon2):
    """Calculates the Haversine distance between two points on Earth."""
    R = 6371  # Earth's radius in km

    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)
    a = sin(dlat / 2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon / 2)**2
    c = 2 * asin(sqrt(a))

    return R * c

if __name__ == '__main__':
    # Example usage
    city = "San Diego, CA"
    latitude, longitude = get_coordinates(city)

    if latitude is not None and longitude is not None:
        nearest_airports = find_nearest_international_airports(latitude, longitude, 1)
        if nearest_airports:
            iata_code = nearest_airports[0][1]['iata']
            print(f"Nearest international airport IATA code to {city}: {iata_code}")
        else:
            print("No international airports found.")
