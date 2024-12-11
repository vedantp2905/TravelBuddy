import requests

class ApiTool:
    def __init__(self, base_url):
        self.base_url = base_url

    def fetch_flight_details(self, origin, destination):
        url = f"{self.base_url}/external-api/test-fetch-flight-details"
        params = {"origin": origin, "destination": destination}
        response = requests.get(url, params=params)
        return response.json()

    def fetch_flight_details2(self, departure_id, arrival_id, outbound_date, return_date):
        url = f"{self.base_url}/external-api/fetch-flight-details"
        params = {
            "departureID": departure_id,
            "arrivalID": arrival_id,
            "outboundDate": outbound_date,
            "returnDate": return_date
        }
        response = requests.get(url, params=params)
        return response.json()

    def fetch_hotel_data(self, city, check_in_date, check_out_date):
        url = f"{self.base_url}/external-api/fetch-hotel-details"
        params = {
            "city": city,
            "checkIn": check_in_date,
            "checkOut": check_out_date
        }
        response = requests.get(url, params=params)
        return response.json()

    def fetch_weather_data(self, city):
        url = f"{self.base_url}/external-api/fetch-weather-data"
        params = {"city": city}
        response = requests.get(url, params=params)
        return response.json()
