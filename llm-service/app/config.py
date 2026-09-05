"""Configuration du service, lue depuis les variables d'environnement.

Volontairement minimale a ce stade (etape 8.1) : aucune valeur sensible
n'est codee en dur. La conteneurisation (8.2) et la degradation controlee
cote Java (8.3) s'appuieront sur les memes variables, chargees ici une
seule fois et exposees via `settings`.
"""

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Cle API Google Gemini. Meme fournisseur que l'EmbeddingClient Java du
    # lot 6 (gemini-embedding-2), mais cle et modele distincts : ce service
    # ne fait pas d'embeddings, seulement de la generation de texte.
    gemini_api_key: str = Field(
        ...,
        description="Cle API Google Gemini, obtenue via Google AI Studio.",
    )
    gemini_model: str = Field(default="gemini-2.5-flash")
    gemini_timeout_seconds: float = Field(default=60.0)
    log_level: str = Field(default="INFO")


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
