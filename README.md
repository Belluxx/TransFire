# 🔥 TransFire

TransFire is a simple tool that allows you to use your locally running LLMs while far from home, whitout requiring port forwarding. TransFire will route an OpenAI compatible API exposed by LMStudio or Ollama through your firebase instance of choice, encrypting all traffic with a pre-shared AES key so not even Google will be able to read your conversations.

## 🔨 Setup
1. Install the APK on your phone
2. Go to the [Firebase console](https://console.firebase.google.com/).
3. Click on `Create a new Firebase project`
4. Proceed through the whole process, opting out of Analytics and Gemini preferably
5. Now on the left panel, expand the `Build` dropdown and select `Realtime Database`
6. Click on `Create Database`
7. Select a database region of your choice
8. Select `Start in locked mode` and proceed
9. Click on the URL icon to copy the database URL. This is your **Firebase RTDB URL**
10. Now clck on the settings icon in the top-left corner and open `Project Settings`
11. Go to the `Service accounts` tab and then click on `Database secrets`
12. You should see one secret in the list, if not click on `Add secret`
13. Hover on the secret to reveal the `Show` button and click it, then copy the key. This is your **Firebase API key**
14. Now go to the app and click on `Get started`
15. Put the `Firebase RTDB URL` and `Firebase API key` in the corresponding fields, then choose an AES password to encrypt the traffic to/from Firebase.
16. Click on `Save configuration` and then `Next`.
17. You can now start the server with the same configuration and chat.

## 🔩 How it works
```mermaid
sequenceDiagram
    participant TransFire App
    participant Firebase
    participant TransFire Server
    participant LLM Server

    TransFire App->>TransFire App: Append user message to chat
    TransFire App->>TransFire App: Encrypt chat
    TransFire App->>Firebase: Send chat
    
    TransFire Server->>Firebase: Get chat
    TransFire Server->>TransFire Server: Decrypt chat
    TransFire Server->>LLM Server: Send chat
    LLM Server->>TransFire Server: Return LLM response
    TransFire Server->>TransFire Server: Encrypt response
    TransFire Server->>Firebase: Send response
    
    TransFire App->>Firebase: Get response
    TransFire App->>TransFire App: Decrypt response
    TransFire App->>TransFire App: Append response to chat
```
