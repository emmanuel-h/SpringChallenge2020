import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// TODO: Player collisions
// TODO: Take care of wrap grid
// TODO: Have an updated map with pellets (remove pellets from map when eat)
// TODO: Switch form when facing an enemy
// TODO: Have an updated map with unvisited floors (go here instead of random move)
class Player {

    static int myScore = 0;
    static int opponentScore = 0;
    static int width; // size of the grid
    static int height; // top left corner is (x=0, y=0)
    static int visiblePacCount; // all your pacs and enemy pacs in sight
    static int visiblePelletCount; // all pellets in sight
    static int[][] map;
    static List<Pellet> pelletsMap;
    static String move;

    public static void main(String args[]) {
        final Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt();
        map = new int[width][height];

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < row.length(); j++) {
                map[j][i] = getMapInt(String.valueOf(row.charAt(j)));
            }
        }

        // game loop
        while (true) {
            Map<Integer, Pac> allyPacs = new HashMap<>();
            Map<Integer, Pac> enemyPacs = new HashMap<>();
            myScore = in.nextInt();
            opponentScore = in.nextInt();
            visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                Pac pac = new Pac();
                pac.id = pacId;
                pac.x = in.nextInt(); // position in the grid
                pac.y = in.nextInt(); // position in the grid
                pac.typeId = in.next(); // unused in wood leagues
                pac.speedTurnsLeft = in.nextInt(); // unused in wood leagues
                pac.abilityCooldown = in.nextInt(); // unused in wood leagues
                if (mine) {
                    allyPacs.put(pacId, pac);
                } else {
                    enemyPacs.put(pacId, pac);
                }
            }
            visiblePelletCount = in.nextInt(); // all pellets in sight
            pelletsMap = new ArrayList<>();
            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth
                pelletsMap.add(new Pellet(x, y, value));
            }
            move = "";
            for (Pac pac: allyPacs.values()) {
                findNextPellet(pac);
            }
            System.out.println(move.substring(1));
        }
    }

    // ALGORITHMS

    static void findNextPellet(Pac pac) {
        Pellet pelletTogo = pelletsMap
                .stream()
                .max(Comparator.comparing(p -> p.isWorth(pac.x, pac.y)))
                .orElseGet(() -> noPelletInSight(pac));
        move += "|MOVE " + pac.id + " " + pelletTogo.x + " " + pelletTogo.y; // MOVE <pacId> <x> <y>
    }

    static Pellet noPelletInSight(Pac pac) {
        if ((pac.x+1 < width) && (map[pac.x + 1][pac.y] == -1)) {
            return new Pellet(pac.x + 1, pac.y);
        }
        if ((pac.y+1 < height) && (map[pac.x][pac.y + 1] == -1)) {
            return new Pellet(pac.x, pac.y + 1);
        }
        if ((pac.x-1 > 0) && (map[pac.x - 1][pac.y] == -1)) {
            return new Pellet(pac.x - 1, pac.y);
        }
        return new Pellet(pac.x, pac.y - 1);
    }

    // UTILS

    static int getMapInt(String mapValue) {
        switch (mapValue) {
            case " ":
                return -1;
            case "#":
                return -2;
            default:
                return Integer.parseInt(mapValue);
        }
    }

    enum Grid {
        FLOOR,
        WALL
    }

    // OBJECTS

    static class Pellet {
        int id;
        int x;
        int y;
        int value;

        public Pellet(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public Pellet(final int x, final int y, final int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public int isWorth(int playerX, int playerY) {
            int worth = value >= 10 ? 1000 : value;
            worth -= Math.abs(this.x - playerX);
            worth -= Math.abs(this.y - playerY);
            return worth;
        }
    }

    static class Pac {
        int id;
        int x;
        int y;
        String typeId;
        int speedTurnsLeft;
        int abilityCooldown;

        @Override
        public String toString() {
            return "Pac{" +
                    "id=" + id +
                    ", x=" + x +
                    ", y=" + y +
                    ", typeId='" + typeId + '\'' +
                    ", speedTurnsLeft=" + speedTurnsLeft +
                    ", abilityCooldown=" + abilityCooldown +
                    '}';
        }
    }
}
