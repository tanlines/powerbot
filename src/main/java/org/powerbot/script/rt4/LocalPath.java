package org.powerbot.script.rt4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.powerbot.bot.rt4.client.Client;
import org.powerbot.bot.rt4.client.CollisionMap;
import org.powerbot.bot.rt4.client.Landscape;
import org.powerbot.script.Locatable;
import org.powerbot.script.Random;
import org.powerbot.script.Tile;

/**
 * LocalPath
 */
public class LocalPath extends Path {
	private final Locatable destination;
	private TilePath tilePath;
	private Tile tile;

	LocalPath(final ClientContext ctx, final Locatable destination) {
		super(ctx);
		this.destination = destination;
	}

	static Graph getGraph(final ClientContext ctx) {
		final Client client = ctx.client();
		if (client == null) {
			return null;
		}
		final int floor = client.getFloor();
		final CollisionMap[] maps = client.getCollisionMaps();
		final CollisionMap map;
		final Tile base = ctx.game.mapOffset();
		if (maps == null || floor < 0 || floor >= maps.length || (map = maps[floor]) == null || base == Tile.NIL) {
			return null;
		}
		final int[][] arr = map.getFlags();
		return arr != null ? new Graph(arr, base.x(), base.y()) : null;
	}

	static void dijkstra(final Graph graph, final Node source, final Node target) {
		source.g = 0d;
		source.f = 0d;

		final Queue<Node> queue = new PriorityQueue<Node>(8, new Comparator<Node>() {
			@Override
			public int compare(final Node o1, final Node o2) {
				return Double.compare(o1.f, o2.f);
			}
		});

		final double sqrt2 = Math.sqrt(2);

		queue.add(source);
		source.opened = true;
		while (!queue.isEmpty()) {
			final Node node = queue.poll();
			node.closed = true;
			if (node.equals(target)) {
				break;
			}
			for (final Node neighbor : graph.neighbors(node)) {
				if (neighbor.closed) {
					continue;
				}
				final double ng = node.g + ((neighbor.x - node.x == 0 || neighbor.y - node.y == 0) ? 1d : sqrt2);

				if (!neighbor.opened || ng < neighbor.g) {
					neighbor.g = ng;
					neighbor.h = 0;//no heuristic
					neighbor.f = neighbor.g + neighbor.h;
					neighbor.parent = node;

					if (!neighbor.opened) {
						queue.offer(neighbor);
						neighbor.opened = true;
					}
				}
			}
		}
	}

	static Node[] follow(Node target) {
		final List<Node> nodes = new LinkedList<Node>();
		if (Double.isInfinite(target.g)) {
			return new Node[0];
		}
		while (target != null) {
			nodes.add(target);
			target = target.parent;
		}

		Collections.reverse(nodes);
		final Node[] path = new Node[nodes.size()];
		return nodes.toArray(path);
	}

	@Override
	public boolean traverse(final EnumSet<TraversalOption> options) {
		return valid() && tilePath.traverse(options);
	}

	@Override
	public Tile next() {
		return valid() ? tilePath.next() : Tile.NIL;
	}

	@Override
	public Tile start() {
		return ctx.players.local().tile();
	}

	@Override
	public Tile end() {
		return destination.tile();
	}

	@Override
	public boolean valid() {
		Tile end = destination.tile();
		if (end == null || end == Tile.NIL) {
			return false;
		}
		if (end.equals(tile) && tilePath != null) {
			return true;
		}
		tile = end;
		Tile start = ctx.players.local().tile();
		final Tile base = ctx.game.mapOffset();
		if (base == Tile.NIL || start == Tile.NIL || end == Tile.NIL) {
			return false;
		}
		start = start.derive(-base.x(), -base.y());
		end = end.derive(-base.x(), -base.y());

		final Graph graph = getGraph(ctx);
		final Node[] path;
		final Node nodeStart, nodeStop;
		if (graph != null &&
				(nodeStart = graph.getNode(start.x(), start.y())) != null &&
				(nodeStop = graph.getNode(end.x(), end.y())) != null) {
			dijkstra(graph, nodeStart, nodeStop);
			path = follow(nodeStop);
			if (path.length > 0) {
				final Tile[] arr = new Tile[path.length];
				for (int i = 0; i < path.length; i++) {
					arr[i] = base.derive(path[i].x, path[i].y);
				}
				tilePath = new TilePath(ctx, arr);
				return true;
			}
		}
		return false;
	}

	static final class Graph {
		private final int offX, offY;
		private final Node[][] nodes;
		private final int width, height;

		private Graph(final int[][] flags, final int offX, final int offY) {
			this.offX = offX;
			this.offY = offY;
			nodes = new Node[flags.length][];
			width = flags.length;
			int height = flags.length;
			for (int x = 0; x < flags.length; x++) {
				final int[] arr = flags[x];
				nodes[x] = new Node[arr.length];
				height = Math.min(height, arr.length);
				for (int y = 0; y < arr.length; y++) {
					nodes[x][y] = new Node(x, y, flags[x][y]);
				}
			}
			this.height = height;
		}

		Node getNode(final int x, final int y) {
			if (x >= 0 && y >= 0 && x < nodes.length && y < nodes[x].length) {
				return nodes[x][y];
			}
			return null;
		}

		private List<Node> neighbors(final Node node) {
			final List<Node> list = new ArrayList<Node>(8);
			final int curr_x = node.x;
			final int curr_y = node.y;
			final int BLOCKED = OBJECT_TILE | OBJECT_BLOCK | DECORATION_BLOCK;
			if (curr_x < 0 || curr_y < 0 ||
					curr_x >= width || curr_y >= height) {
				return list;
			}
			if (curr_y > 0 &&
					(nodes[curr_x][curr_y].flag & WALL_SOUTH) == 0 &&
					(nodes[curr_x][curr_y - 1].flag & BLOCKED) == 0) {
				list.add(nodes[curr_x][curr_y - 1]);
			}
			if (curr_x > 0 &&
					(nodes[curr_x][curr_y].flag & WALL_WEST) == 0 &&
					(nodes[curr_x - 1][curr_y].flag & BLOCKED) == 0) {
				list.add(nodes[curr_x - 1][curr_y]);
			}
			if (curr_y < height - 1 &&
					(nodes[curr_x][curr_y].flag & WALL_NORTH) == 0 &&
					(nodes[curr_x][curr_y + 1].flag & BLOCKED) == 0) {
				list.add(nodes[curr_x][curr_y + 1]);
			}
			if (curr_x < width - 1 &&
					(nodes[curr_x][curr_y].flag & WALL_EAST) == 0 &&
					(nodes[curr_x + 1][curr_y].flag & BLOCKED) == 0) {
				list.add(nodes[curr_x + 1][curr_y]);
			}
			if (curr_x > 0 && curr_y > 0 &&
					(nodes[curr_x][curr_y].flag & (WALL_SOUTHWEST | WALL_SOUTH | WALL_WEST)) == 0 &&
					(nodes[curr_x - 1][curr_y - 1].flag & BLOCKED) == 0 &&
					(nodes[curr_x][curr_y - 1].flag & (WALL_WEST | BLOCKED)) == 0 &&
					(nodes[curr_x - 1][curr_y].flag & (WALL_SOUTH | BLOCKED)) == 0) {
				list.add(nodes[curr_x - 1][curr_y - 1]);
			}
			if (curr_x > 0 && curr_y < height - 1 &&
					(nodes[curr_x][curr_y].flag & (WALL_NORTHWEST | WALL_NORTH | WALL_WEST)) == 0 &&
					(nodes[curr_x - 1][curr_y + 1].flag & BLOCKED) == 0 &&
					(nodes[curr_x][curr_y + 1].flag & (WALL_WEST | BLOCKED)) == 0 &&
					(nodes[curr_x - 1][curr_y].flag & (WALL_NORTH | BLOCKED)) == 0) {
				list.add(nodes[curr_x - 1][curr_y + 1]);
			}
			if (curr_x < height - 1 && curr_y > 0 &&
					(nodes[curr_x][curr_y].flag & (WALL_SOUTHEAST | WALL_SOUTH | WALL_EAST)) == 0 &&
					(nodes[curr_x + 1][curr_y - 1].flag & BLOCKED) == 0 &&
					(nodes[curr_x][curr_y - 1].flag & (WALL_EAST | BLOCKED)) == 0 &&
					(nodes[curr_x + 1][curr_y].flag & (WALL_SOUTH | BLOCKED)) == 0) {
				list.add(nodes[curr_x + 1][curr_y - 1]);
			}
			if (curr_x < width - 1 && curr_y < height - 1 &&
					(nodes[curr_x][curr_y].flag & (WALL_NORTHEAST | WALL_NORTH | WALL_EAST)) == 0 &&
					(nodes[curr_x + 1][curr_y + 1].flag & BLOCKED) == 0 &&
					(nodes[curr_x][curr_y + 1].flag & (WALL_EAST | BLOCKED)) == 0 &&
					(nodes[curr_x + 1][curr_y].flag & (WALL_NORTH | BLOCKED)) == 0) {
				list.add(nodes[curr_x + 1][curr_y + 1]);
			}
			return list;
		}
	}

	static final class Node {
		public final int x, y;
		public final int flag;
		private boolean opened, closed;
		private Node parent;
		private double f, g, h;

		private Node(final int x, final int y, final int flag) {
			this.x = x;
			this.y = y;
			this.flag = flag;
			reset();
		}

		private void reset() {
			opened = closed = false;
			parent = null;
			f = g = h = Double.POSITIVE_INFINITY;
		}

		@Override
		public String toString() {
			return Node.class.getSimpleName() + "[x=" + x + ",y=" + y + "]";
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Node)) {
				return false;
			}
			final Node n = (Node) o;
			return x == n.x && y == n.y;
		}
	}
}
