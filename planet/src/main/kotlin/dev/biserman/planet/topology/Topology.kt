package dev.biserman.planet.topology

import dev.biserman.planet.geometry.MutEdge
import dev.biserman.planet.geometry.MutMesh
import dev.biserman.planet.geometry.MutTri
import dev.biserman.planet.geometry.MutVertex
import dev.biserman.planet.geometry.copy
import godot.global.GD

// Adapted from Andy Gainey, original license below:
// Copyright © 2014 Andy Gainey <andy@experilous.com>
//
// Usage of the works is permitted provided that this instrument
// is retained with the works, so that any entity that uses the
// works is notified of this instrument.
//
// DISCLAIMER: THE WORKS ARE WITHOUT WARRANTY.

class Topology(val tiles: List<Tile>, val borders: List<Border>, val corners: List<Corner>) {
    // this doesn't fully link the geometries to each other yet. also has duplicate verts & edges
    val mesh by lazy {
        val verts = mutableListOf<MutVertex>()
        val edges = mutableListOf<MutEdge>()
        val tris = mutableListOf<MutTri>()

        for (tile in tiles) {
            val startVertIndex = verts.size
            verts.addAll(tile.corners.map { MutVertex(it.position) })
            edges.addAll((1..<tile.corners.size).map {
                MutEdge(
                    mutableListOf(
                        startVertIndex + it - 1,
                        startVertIndex + it
                    )
                )
            })
            tris.addAll((2..<tile.corners.size).map {
                MutTri(
                    mutableListOf(
                        startVertIndex,
                        startVertIndex + it - 1,
                        startVertIndex + it,
                    )
                )
            })
        }

        return@lazy MutMesh(verts, edges, tris)
    }
}

fun (MutMesh).toTopology(): Topology {
    val borders = this.edges.withIndex().map { (i, edge) -> MutBorder(i) }
    val tiles = this.verts.withIndex().map { (i, tile) -> MutTile(i, tile.position.copy()) }
    val corners = this.tris.withIndex()
        .map { (i, tri) ->
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