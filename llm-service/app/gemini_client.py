"""Appel a l'API Gemini en streaming (generation de texte).

Isole du reste de l'application : `main.py` ne connait que la fonction
`stream_completion` et l'exception `GeminiError`, jamais le format de
reponse propre a Gemini. Remplacer de fournisseur LLM plus tard reviendrait
a ecrire un module equivalent, sans toucher au reste du service.
"""

import json
import logging
from typing import AsyncIterator

import httpx

from .config import settings

logger = logging.getLogger("llm-service.gemini")

_STREAM_URL_TEMPLATE = (
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent"
)


class GeminiError(Exception):
    """Erreur remontee par l'appel a Gemini (reseau, HTTP ou format de reponse)."""


async def stream_completion(
    prompt: str,
    *,
    temperature: float = 0.3,
    max_output_tokens: int = 1024,
) -> AsyncIterator[str]:
    """Interroge Gemini en streaming et cede les fragments de texte recus.

    Utilise `streamGenerateContent?alt=sse`, qui renvoie un flux SSE natif
    (lignes `data: {...}`) plutot que du JSON chunk par chunk : c'est ce qui
    permet de relayer la reponse au fur et a mesure sans attendre la fin de
    la generation.
    """
    url = _STREAM_URL_TEMPLATE.format(model=settings.gemini_model)
    payload = {
        "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": temperature,
            "maxOutputTokens": max_output_tokens,
        },
    }
    params = {"alt": "sse", "key": settings.gemini_api_key}

    try:
        async with httpx.AsyncClient(timeout=settings.gemini_timeout_seconds) as client:
            async with client.stream("POST", url, params=params, json=payload) as response:
                if response.status_code != 200:
                    body = await response.aread()
                    raise GeminiError(
                        f"Gemini a repondu {response.status_code} : "
                        f"{body.decode(errors='replace')}"
                    )

                async for line in response.aiter_lines():
                    if not line or not line.startswith("data:"):
                        continue
                    data = line[len("data:"):].strip()
                    if not data or data == "[DONE]":
                        continue
                    try:
                        chunk = json.loads(data)
                    except json.JSONDecodeError:
                        logger.warning("Fragment SSE Gemini illisible, ignore : %r", data)
                        continue

                    for candidate in chunk.get("candidates", []):
                        for part in candidate.get("content", {}).get("parts", []):
                            text = part.get("text")
                            if text:
                                yield text
    except httpx.HTTPError as exc:
        raise GeminiError(f"Appel a Gemini impossible : {exc}") from exc
