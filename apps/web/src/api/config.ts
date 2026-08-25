export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
export const MAP_TILE_URL = import.meta.env.VITE_MAP_TILE_URL
  ?? 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
export const MAP_TILE_ATTRIBUTION = import.meta.env.VITE_MAP_TILE_ATTRIBUTION
  ?? '&copy; OpenStreetMap contributors'
export const GEOAPIFY_API_KEY = import.meta.env.VITE_GEOAPIFY_API_KEY
  ?? new URL(MAP_TILE_URL.replace('{z}', '0').replace('{x}', '0').replace('{y}', '0'), window.location.href).searchParams.get('apiKey')
  ?? ''
