import json
import time
import base64
import hashlib
from cryptography.hazmat.primitives import hashes
import requests
from dotenv import dotenv_values
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.backends import default_backend

# Load configuration from .env without polluting os.environ
config = dotenv_values(".env")

FIREBASE_URL = config["FIREBASE_URL"]
FIREBASE_API_KEY = config["FIREBASE_API_KEY"]
ENCRYPTION_PASSWORD = config["ENCRYPTION_PASSWORD"]
OPENAI_LIKE_API_URL = config["OPENAI_LIKE_API_URL"]
OPENAI_LIKE_API_KEY = config["OPENAI_LIKE_API_KEY"]
POLL_INTERVAL = int(config["POLL_INTERVAL"])

KEY_CLIENT_MAILBOX = "mailbox_client"
KEY_SERVER_MAILBOX = "mailbox_server"

TEMPLATE_KEY = '{{!MESSAGE!}}'
QUICK_MESSAGE_TEMPLATE = "{'id': 'chatcmpl-quick-message', 'object': 'chat.completion', 'created': 0, 'model': 'quick-message', 'choices': [{'index': 0, 'message': {'role': 'assistant', 'content': '" + TEMPLATE_KEY + "', 'tool_calls': []}, 'logprobs': None, 'finish_reason': 'stop'}], 'usage': {'prompt_tokens': 0, 'completion_tokens': 0, 'total_tokens': 0}, 'stats': {}, 'system_fingerprint': 'quick-message'}"

def derive_salt(password: str) -> bytes:
    return hashlib.sha256(password.encode('utf-8')).digest()[:16]


def derive_key(password: str) -> bytes:
    salt = derive_salt(password)
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=60000,
        backend=default_backend()
    )
    return kdf.derive(password.encode('utf-8'))


def decrypt_data(password: str, ciphertext_b64: str, iv_b64: str) -> str:
    key = derive_key(password)
    ciphertext = base64.b64decode(ciphertext_b64)
    iv = base64.b64decode(iv_b64)
    
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    plaintext = decryptor.update(ciphertext) + decryptor.finalize()
    
    # Remove PKCS5 padding
    padding_length = plaintext[-1]
    plaintext = plaintext[:-padding_length]
    
    return plaintext.decode('utf-8')


def encrypt_data(password: str, plaintext: str) -> tuple:
    import os
    key = derive_key(password)
    iv = os.urandom(16)
    
    # Add PKCS5 padding
    padding_length = 16 - (len(plaintext) % 16)
    plaintext_bytes = plaintext.encode('utf-8') + bytes([padding_length] * padding_length)
    
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(plaintext_bytes) + encryptor.finalize()
    
    return base64.b64encode(ciphertext).decode('utf-8'), base64.b64encode(iv).decode('utf-8')


def firebase_get(path: str) -> dict:
    url = f"{FIREBASE_URL}/{path}.json?auth={FIREBASE_API_KEY}"
    response = requests.get(url)
    response.raise_for_status()
    return response.json()


def firebase_post(path: str, data: dict) -> dict:
    url = f"{FIREBASE_URL}/{path}.json?auth={FIREBASE_API_KEY}"
    response = requests.post(url, json=data)
    response.raise_for_status()
    return response.json()


def firebase_delete(path: str):
    url = f"{FIREBASE_URL}/{path}.json?auth={FIREBASE_API_KEY}"
    response = requests.delete(url)
    response.raise_for_status()


def call_openai_api(messages: list, model: str) -> requests.Response:
    headers = {
        "Authorization": f"Bearer {OPENAI_LIKE_API_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "model": model,
        "messages": messages
    }
    
    response = requests.post(
        url=f"{OPENAI_LIKE_API_URL}/v1/chat/completions",
        json=payload,
        headers=headers
    )
    
    return response


def process_request():
    try:
        # Check client mailbox
        data = firebase_get(KEY_CLIENT_MAILBOX)
        
        if data is None:
            return
        
        # Extract the pushed data
        push_key = next(iter(data.keys()))
        encrypted_payload = data[push_key]
        
        # Decrypt the request
        payload_json = json.loads(encrypted_payload)
        decrypted_data = decrypt_data(
            ENCRYPTION_PASSWORD,
            payload_json['data'],
            payload_json['iv']
        )
        
        decrypted_data = decrypted_data.replace('\n', '\\n')
        request_data = json.loads(decrypted_data)
        print(f"Received request for model: {request_data['model']}")
        
        # Call OpenAI API
        api_response = call_openai_api(request_data['messages'], request_data['model'])
        api_response_code = api_response.status_code
        if (api_response_code != 200):
            if (api_response_code == 404):
                send_quick_message("**ERROR**: Model not found")
                return
        
        api_response_json = api_response.json()
        print(api_response_json)
        
        # Encrypt the response
        response_str = json.dumps(api_response_json)
        encrypted_response, iv = encrypt_data(ENCRYPTION_PASSWORD, response_str)
        
        encrypted_data = json.dumps({
            "data": encrypted_response,
            "iv": iv
        })
        
        # Post response to server mailbox
        firebase_post(KEY_SERVER_MAILBOX, encrypted_data)
        print("Response sent successfully")
        
        # Clean up client mailbox
        firebase_delete(KEY_CLIENT_MAILBOX)
    except UnicodeDecodeError as e:
        send_quick_message("**Error**")
        print(f"Error decoding message: {e}")
    except Exception as e:
        send_quick_message("Unknown processing request:\n\n```\n{e}\n```")
        print(f"Error processing request: {e}")


def send_quick_message(message: str):
    message = message.replace('\n', '\\n').replace('"', '\\"')
    encrypted_response, iv = encrypt_data(ENCRYPTION_PASSWORD, QUICK_MESSAGE_TEMPLATE.replace(TEMPLATE_KEY, message))
    encrypted_data = json.dumps({
        "data": encrypted_response,
        "iv": iv
    })
    firebase_post(KEY_SERVER_MAILBOX, encrypted_data)
    firebase_delete(KEY_CLIENT_MAILBOX)


def main():
    print("TransFire server started")
    print(f"Polling every {POLL_INTERVAL} seconds...")
    
    while True:
        process_request()
        time.sleep(POLL_INTERVAL)


if __name__ == "__main__":
    main()
