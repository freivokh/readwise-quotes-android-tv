// app/src/main/java/com/readwisequotes/ui/TokenEntryServer.kt
package com.readwisequotes.ui

import fi.iki.elonen.NanoHTTPD

/**
 * Simple HTTP server for receiving API token from a phone on the local network.
 * User visits the TV's IP address in their phone browser and pastes their token.
 */
class TokenEntryServer(
    port: Int = 8080,
    private val onTokenReceived: (String) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.method) {
            Method.GET -> serveForm()
            Method.POST -> handleTokenSubmission(session)
            else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }
    }

    private fun serveForm(): Response {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Readwise Quotes Setup</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                        min-height: 100vh;
                        margin: 0;
                        padding: 20px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .container {
                        background: rgba(255, 255, 255, 0.1);
                        backdrop-filter: blur(10px);
                        border-radius: 16px;
                        padding: 32px;
                        max-width: 400px;
                        width: 100%;
                        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    }
                    h1 {
                        color: #fff;
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        text-align: center;
                    }
                    .subtitle {
                        color: rgba(255, 255, 255, 0.7);
                        font-size: 14px;
                        text-align: center;
                        margin-bottom: 24px;
                    }
                    label {
                        color: rgba(255, 255, 255, 0.9);
                        font-size: 14px;
                        display: block;
                        margin-bottom: 8px;
                    }
                    input[type="text"] {
                        width: 100%;
                        padding: 14px;
                        border: 2px solid rgba(255, 255, 255, 0.2);
                        border-radius: 8px;
                        background: rgba(255, 255, 255, 0.1);
                        color: #fff;
                        font-size: 16px;
                        margin-bottom: 16px;
                        transition: border-color 0.2s;
                    }
                    input[type="text"]:focus {
                        outline: none;
                        border-color: #4a9eff;
                    }
                    input[type="text"]::placeholder {
                        color: rgba(255, 255, 255, 0.4);
                    }
                    button {
                        width: 100%;
                        padding: 14px;
                        background: #4a9eff;
                        color: #fff;
                        border: none;
                        border-radius: 8px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    button:hover {
                        background: #3a8eef;
                    }
                    .help {
                        color: rgba(255, 255, 255, 0.6);
                        font-size: 12px;
                        text-align: center;
                        margin-top: 16px;
                    }
                    .help a {
                        color: #4a9eff;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📚 Readwise Quotes</h1>
                    <p class="subtitle">Enter your API token to complete TV setup</p>
                    <form method="POST">
                        <label for="token">API Token</label>
                        <input type="text" id="token" name="token" placeholder="Paste your token here" autocomplete="off" autofocus>
                        <button type="submit">Send to TV</button>
                    </form>
                    <p class="help">Get your token at <a href="https://readwise.io/access_token" target="_blank">readwise.io/access_token</a></p>
                </div>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun handleTokenSubmission(session: IHTTPSession): Response {
        val files = mutableMapOf<String, String>()
        session.parseBody(files)

        val token = session.parms["token"]?.trim()

        if (token.isNullOrEmpty()) {
            return serveResult(false, "No token provided")
        }

        // Notify the callback
        onTokenReceived(token)

        return serveResult(true, "Token sent to TV!")
    }

    private fun serveResult(success: Boolean, message: String): Response {
        val color = if (success) "#4ade80" else "#f87171"
        val icon = if (success) "✓" else "✗"

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Readwise Quotes Setup</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                        min-height: 100vh;
                        margin: 0;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .result {
                        text-align: center;
                        color: #fff;
                    }
                    .icon {
                        font-size: 64px;
                        color: $color;
                        margin-bottom: 16px;
                    }
                    .message {
                        font-size: 20px;
                    }
                    .hint {
                        color: rgba(255, 255, 255, 0.6);
                        font-size: 14px;
                        margin-top: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="result">
                    <div class="icon">$icon</div>
                    <div class="message">$message</div>
                    ${if (success) "<p class=\"hint\">You can close this page now.</p>" else "<p class=\"hint\"><a href=\"/\" style=\"color: #4a9eff;\">Try again</a></p>"}
                </div>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }
}
