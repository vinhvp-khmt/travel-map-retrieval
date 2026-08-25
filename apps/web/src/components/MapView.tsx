import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import { MAP_TILE_ATTRIBUTION, MAP_TILE_URL } from '../api/config'
import type { SearchResult, UserLocation } from '../types/search'

type Cluster = { latitude: number; longitude: number; items: SearchResult[] }

export function clusterResults(results: SearchResult[], precision = 0.01): Cluster[] {
  const buckets = new Map<string, Cluster>()
  results.forEach((item) => {
    const key = `${Math.round(item.latitude / precision)}:${Math.round(item.longitude / precision)}`
    const cluster = buckets.get(key) ?? { latitude: item.latitude, longitude: item.longitude, items: [] }
    cluster.items.push(item)
    buckets.set(key, cluster)
  })
  return [...buckets.values()]
}

export function MapView({ results, selectedId, userLocation, onSelect }: {
  results: SearchResult[]; selectedId?: string; userLocation?: UserLocation; onSelect: (poi: SearchResult) => void
}) {
  const container = useRef<HTMLDivElement>(null)
  const map = useRef<L.Map | null>(null)
  const overlay = useRef<L.LayerGroup | null>(null)
  const [tileError, setTileError] = useState(false)

  useEffect(() => {
    if (!container.current || map.current) return
    map.current = L.map(container.current, { zoomControl: false }).setView([16.0471, 108.2068], 6)
    L.control.zoom({ position: 'bottomright' }).addTo(map.current)
    L.tileLayer(MAP_TILE_URL, {
      attribution: MAP_TILE_ATTRIBUTION, maxZoom: 19,
    }).on('tileerror', () => setTileError(true)).addTo(map.current)
    overlay.current = L.layerGroup().addTo(map.current)
    return () => { map.current?.remove(); map.current = null }
  }, [])

  useEffect(() => {
    if (!map.current || !overlay.current) return
    overlay.current.clearLayers()
    const fitPoints: L.LatLngTuple[] = []
    if (userLocation) {
      const userPoint: L.LatLngTuple = [userLocation.latitude, userLocation.longitude]
      fitPoints.push(userPoint)
      L.circle(userPoint, {
        radius: userLocation.accuracyMeters ?? 80,
        color: '#174a35',
        fillColor: '#174a35',
        fillOpacity: 0.08,
        weight: 1,
      }).addTo(overlay.current)
      L.circleMarker(userPoint, {
        radius: 8,
        color: '#ffffff',
        fillColor: '#174a35',
        fillOpacity: 1,
        weight: 3,
      }).bindTooltip('Vị trí của bạn').addTo(overlay.current)
    }
    const clusters = clusterResults(results)
    clusters.forEach((cluster) => {
      const first = cluster.items[0]
      const selected = cluster.items.some((item) => item.poiId === selectedId)
      fitPoints.push([cluster.latitude, cluster.longitude])
      const icon = L.divIcon({
        className: '',
        html: `<button class="map-marker ${selected ? 'selected' : ''}" aria-label="${first.name}">${cluster.items.length > 1 ? cluster.items.length : '●'}</button>`,
        iconSize: [40, 40], iconAnchor: [20, 20],
      })
      L.marker([cluster.latitude, cluster.longitude], { icon }).addTo(overlay.current!)
        .on('click', () => onSelect(first))
    })
    if (fitPoints.length > 1) {
      const bounds = L.latLngBounds(fitPoints)
      map.current.fitBounds(bounds.pad(0.25), { maxZoom: 16 })
    } else if (fitPoints.length === 1) {
      map.current.setView(fitPoints[0], 15)
    }
  }, [results, selectedId, userLocation, onSelect])

  return <>
    <div className="map-view" ref={container} aria-label="Bản đồ kết quả" />
    {tileError && <div className="map-warning">Tile online chưa tải được. Kiểm tra network hoặc Geoapify key.</div>}
  </>
}
