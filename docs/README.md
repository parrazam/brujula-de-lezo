# Sitio web de Brújula de Lezo

Sitio público servido vía GitHub Pages desde esta carpeta `docs/`.

🌐 **https://brujuladelezo.cuzo.dev/**

Contiene:

- `index.html` — landing principal
- `privacy.html` — política de privacidad (URL exigida por Google Play Console)
- `styles.css` — paleta exacta de la app (Siglo de Oro: azul marino imperial, oro viejo, rojo borgoña, blanco pergamino)
- `compass_face.png` / `compass_needle.png` — rosa de los vientos y aguja, traídas del `design-system` de la app
- `icon.png` — icono de la app (og:image y apple-touch-icon)
- `favicon.svg` — favicon con la estrella de la brújula

## Stack

HTML + CSS sin frameworks ni build. El tema claro/oscuro se decide por `prefers-color-scheme` y puede alternarse con el botón de la cabecera.

## Desarrollo local

```bash
cd docs
python3 -m http.server 8000
# abrir http://localhost:8000
```

## Despliegue

Configurado en **Settings → Pages**:

- Source: `Deploy from a branch`
- Branch: `master` / `/docs`

El dominio personalizado (`brujuladelezo.cuzo.dev`) está fijado en el fichero `CNAME`.
El deploy es automático en cada push a `master` que toque `docs/`.

## Tipografías

- [Fraunces](https://fonts.google.com/specimen/Fraunces) — display
- [IBM Plex Sans](https://fonts.google.com/specimen/IBM+Plex+Sans) — body
- [JetBrains Mono](https://fonts.google.com/specimen/JetBrains+Mono) — detalles

Servidas vía Google Fonts.
