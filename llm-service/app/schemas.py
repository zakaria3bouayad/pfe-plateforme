"""Schemas Pydantic des requetes/reponses exposees par l'API."""

from pydantic import BaseModel, Field


class CompletionRequest(BaseModel):
    """Corps de `POST /completion`.

    Le prompt est deja entierement construit cote Java par `RagService`
    (contexte des k=5 passages, role, garde-fous) au moment ou il arrive
    ici : ce service ne fait aucune recherche vectorielle ni construction
    de prompt, seulement l'appel au modele en streaming.
    """

    prompt: str = Field(..., min_length=1)
    temperature: float = Field(default=0.3, ge=0.0, le=2.0)
    max_output_tokens: int = Field(default=1024, ge=1, le=8192)
