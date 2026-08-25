import { describe, expect, it } from 'vitest'
import { clusterResults } from '../components/MapView'
import { cafe } from './fixtures'

describe('MapView marker clustering', () => {
  it('clusters close results into one synchronized marker bucket', () => {
    const neighbour = { ...cafe, poiId: 'neighbour', latitude: cafe.latitude + 0.001 }
    const clusters = clusterResults([cafe, neighbour])
    expect(clusters).toHaveLength(1)
    expect(clusters[0].items).toHaveLength(2)
  })
})
