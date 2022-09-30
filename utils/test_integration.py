import unittest
import os
import requests
import json


API_URL = 'http://localhost:8080/process'


def create_payload(text):
    return {"type": "text", "content": text}


def create_payload_with_params(text, params):
    return {"type": "text", "content": text, "params": params}


def call_api(payload):
    headers = {'Content-Type': 'application/json'}
    payload = json.dumps(payload)
    return requests.post(
            API_URL, headers=headers, data=payload).json()


class TestIntegration(unittest.TestCase):

    def setUp(self):
        self.text = "Hello, world!"

    def test_response_type(self):
        payload = create_payload(self.text)
        response = call_api(payload)["response"]
        self.assertEqual(response.get("type"), "classification")

    def test_response_content(self):
        payload = create_payload(self.text)
        response = call_api(payload)["response"]
        self.assertGreater(len(response.get("classes")), 0)

    def test_text_empty(self):
        payload = create_payload("")
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_text_whitespace(self):
        payload = create_payload(" \t\t  \n ")
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_text_long(self):
        long_text = self.text * 10000
        payload = create_payload(long_text)
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_token_long(self):
        long_token = "å" * 10000
        payload = create_payload(long_token)
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_characters_special(self):
        spec_text = "\N{grinning face}\u4e01\u0009" + self.text + "\u0008"
        payload = create_payload(spec_text)
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_language_unsupported(self):
        # Udi is not in languagelist.
        wrong_lang = "Са пасч'агъэн са пасч'агъаx ч'аxпи."
        payload = create_payload(wrong_lang)
        response = call_api(payload)["response"]
        self.assertIn("classes", response)

    def test_parameters_valid(self):
        params = {"nbest": 2, "languages": ["fin","swe","eng"]}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)["response"]
        self.assertEqual(len(response["classes"]), 2)

    def test_parameters_invalid_names(self):
        params = {"Nbest": 2, "Languages": ["fin","swe","eng"]}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)["response"]
        self.assertGreater(len(response.get("classes")), 2)

    def test_parameters_none_nbest(self):
        params = {"nbest": None}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)
        self.assertEqual(response["failure"]["errors"][0]["code"],
                         "heli.parameter.invalid")

    def test_parameters_invalid_type_nbest(self):
        params = {"nbest": 2.0}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)
        self.assertEqual(response["failure"]["errors"][0]["code"],
                         "heli.parameter.invalid")
        
    def test_parameters_none_languages(self):
        params = {"languages": None}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)
        self.assertEqual(response["failure"]["errors"][0]["code"],
                         "elg.service.internalError")

    def test_parameters_invalid_type_languages(self):
        params = {"languages": "fin,swe,eng"}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)
        self.assertEqual(response["failure"]["errors"][0]["code"],
                         "elg.service.internalError")

    def test_parameters_invalid_languages(self):
        params = {"languages": ["fin", "xxx"]}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)
        self.assertEqual(response["failure"]["errors"][0]["code"],
                         "heli.parameter.invalid")
    
    def test_parameters_empty(self):
        params = {}
        payload = create_payload_with_params(self.text, params)
        response = call_api(payload)["response"]
        self.assertGreater(len(response.get("classes")), 0)


if __name__ == '__main__':
    unittest.main()
