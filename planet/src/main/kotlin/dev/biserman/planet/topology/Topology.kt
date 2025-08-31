package dev.biserman.planet.topology

import com.github.davidmoten.rtreemulti.Entry
import com.github.davidmoten.rtreemulti.RTree
import com.github.davidmoten.rtreemulti.geometry.Point
import dev.biserman.planet.geometry.*

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.

class Topology(val tiles: List<Tile>, val borders: List<Border>, val corners: List<Corner>) {
	val rTree = tiles.toRTree { it.position.toPoint() }
	val averageRadius by lazy {
		val radii = tiles.flatMap { it.corners.map { corner -> corner.position.distanceTo(it.position) } }
		radii.average()
	}

	// this doesn't fully link the geometries to each other yet. also has duplicate verts & edges
	fun makeMesh(): MutMesh {
		val mutMesh = MutMesh(mutableListOf(), mutableListOf(), mutableListOf())

		for (tile in tiles) {
			val startVertIndex = mutMesh.verts.size
			mutMesh.verts.add(MutVertex(centroid(tile.corners.map { it.position })))
			mutMesh.verts.addAll(tile.corners.map { MutVertex(it.position) })

			mutMesh.edges.addAll((2..tile.corners.size).map {
				MutEdge(
					mutableListOf(
						startVertIndex + it - 1, startVertIndex + it
					)
				)
			})
			mutMesh.edges.add(
				MutEdge(
					mutableListOf(
						startVertIndex + tile.corners.size, startVertIndex + 1
					)
				)
			)

			mutMesh.tris.addAll((2..tile.corners.size).map {
				MutTri(
					mutableListOf(
						startVertIndex,
						startVertIndex + it - 1,
						startVertIndex + it,
					)
				)
			})
			mutMesh.tris.add(
				MutTri(
					mutableListOf(
						startVertIndex, startVertIndex + tile.corners.size, startVertIndex + 1
					)
				)
			)
		}

		return mutMesh
	}
}

fun (MutMesh).toTopology(): Topology {
	val borders = this.edges.withIndex().map { (i, edge) -> MutBorder(i) }
	val tiles = this.verts.withIndex().map { (i, tile) -> MutTile(i) }
	val corners = this.tris.withIndex().map { (i, tri) ->
		MutCorner(
			i,
			tri.centroid(this),
			borders = tri.edgeIndexes.map { borders[it] }.toMutableList(),
			tiles = tri.vertIndexes.map { tiles[it] }.toMutableList()
		)
	}

	// link borders to corners and tiles
	for (i in 0..<borders.size) {
		val border = borders[i]
		val edge = this.edges[i]
		for (triIndex in edge.triIndexes) {
			val corner = corners[triIndex]
			border.corners.add(corner)
			for (cornerBorder in corner.borders) {
				if (cornerBorder != border) {
					border.borders.add(cornerBorder)
				}
			}
		}

		edge.vertIndexes.mapTo(border.tiles) { tiles[it] }
	}

	// link corners to each other
	for (corner in corners) {
		corner.borders.mapTo(corner.corners) { it.oppositeCorner(corner) }
	}

	// link tiles to borders and adjacent tiles
	for (i in 0..<tiles.size) {
		val tile = tiles[i]
		val vert = this.verts[i]

		vert.triIndexes.mapTo(tile.corners) { corners[it] }

		for (edgeIndex in vert.edgeIndexes) {
			val border = borders[edgeIndex]
			if (border.tiles[0] == tile) {
				for (j in 0..<tile.corners.size) {
					val corner0 = tile.corners[j]
					val corner1 = tile.corners[(j + 1) % tile.corners.size]
					if (border.corners[1] == corner0 && border.corners[0] == corner1) {
						border.corners[0] = corner0
						border.corners[1] = corner1
					} else if (border.corners[0] != corner0 || border.corners[1] != corner1) {
						continue
					}

					tile.borders.add(border)
					tile.tiles.add(border.oppositeTile(tile))
					break
				}
			} else {
				for (j in 0..<tile.corners.size) {
					val corner0 = tile.corners[j]
					val corner1 = tile.corners[(j + 1) % tile.corners.size]
					if (border.corners[0] == corner0 && border.corners[1] == corner1) {
						border.corners[1] = corner0
						border.corners[0] = corner1
					} else if (border.corners[1] != corner0 || border.corners[0] != corner1) {
						continue
					}

					tile.borders.add(border)
					tile.tiles.add(border.oppositeTile(tile))
					break
				}
			}
		}
	}

	return Topology(tiles, borders, corners)
}
