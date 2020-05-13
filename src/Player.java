import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Take care of wrap grid
// TODO: Change Manhattan distance + instead of a Pac searching closest Case, put in the Case the distance with each Pacman
// TODO: Decrease pound of pellet next to another pac (almost done)
// TODO: When too close of an ally, move away from his target / choose a different target
class Player {

    public static final String SCISSORS = "SCISSORS";
    public static final String ROCK = "ROCK";
    public static final String PAPER = "PAPER";
    static int myScore = 0;
    static int opponentScore = 0;
    static int width; // size of the grid
    static int height; // top left corner is (x=0, y=0)
    static int visiblePacCount; // all your pacs and enemy pacs in sight
    static int visiblePelletCount; // all pellets in sight
    static String move;

    static Map<Integer, Pac> allyPacs = new HashMap<>();
    static Map<Integer, Pac> enemyPacs = new HashMap<>();
    static Map<Integer, Pac> allyPacsLastMove = new HashMap<>();

    static Set<Case> superPellets = new HashSet<>();

    static Case[][] map;

    static int turn;

    public static void main(final String[] args) {
        final Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt();
        map = new Case[width][height];

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            final String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < row.length(); j++) {
                map[j][i] = ' ' == row.charAt(j)
                        ? new Case(Grid.FLOOR, j, i, 1, 0)
                        : new Case(Grid.WALL, j, i, 0, 0);
            }
        }

        // game loop
        for (turn = 0; turn < 200; turn++) {
            Set<Case> superPelletsThisTurn = new HashSet<>();
            allyPacs = new HashMap<>();
            enemyPacs = new HashMap<>();
            final Set<Case> visiblePellets = new HashSet<>();
            myScore = in.nextInt();
            opponentScore = in.nextInt();
            visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            if (turn != 0) {
                incrementLastSeen();
            }
            for (int i = 0; i < visiblePacCount; i++) {
                final int pacId = in.nextInt(); // pac number (unique within a team)
                final boolean mine = in.nextInt() != 0; // true if this pac is yours
                final Pac pac = new Pac();
                pac.id = pacId;
                pac.x = in.nextInt();
                pac.y = in.nextInt();
                pac.typeId = in.next();
                pac.speedTurnsLeft = in.nextInt();
                pac.abilityCooldown = in.nextInt();
                if (mine && !"DEAD".equals(pac.typeId)) {
                    allyPacs.put(pacId, pac);
                } else if (!"DEAD".equals(pac.typeId)) {
                    enemyPacs.put(pacId, pac);
                }
                map[pac.x][pac.y].value = 0;
            }
            visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                final int x = in.nextInt();
                final int y = in.nextInt();
                final int value = in.nextInt(); // amount of points this pellet is worth
                final Case aCase = new Case(Grid.FLOOR, x, y, value, turn);
                map[x][y].turnLastSeen = 0;
                visiblePellets.add(aCase);
                if (value == 10) {
                    map[x][y].value = value;
                    superPelletsThisTurn.add(map[x][y]);
                }

            }
            removeNonExistentPelletsInSight(visiblePellets);
            removeNonExistentSuperPellets(superPelletsThisTurn);
            findClosestPac();
            move = "";
            for (final Pac pac : allyPacs.values()) {
                chooseMove(pac);
            }
            System.out.println(move.substring(1));
            allyPacsLastMove = allyPacs;
        }
    }

    // ALGORITHMS

    static void removeNonExistentSuperPellets(final Set<Case> superPelletsThisTurn) {
        if (turn != 0) {
            superPellets.removeAll(superPelletsThisTurn);
            superPellets.forEach(c -> map[c.x][c.y].value = 0);
        }
        superPellets = superPelletsThisTurn;
    }

    static void findClosestPac() {
        Arrays.stream(map)
                .flatMap(Arrays::stream)
                .forEach(Case::setClosestPac);
    }

    static void removeNonExistentPelletsInSight(final Set<Case> visiblePellets) {
        for (final Pac pac : allyPacs.values()) {
            int x = pac.x + 1;
            int y = pac.y;
            while (x < width && map[x][y].type == Grid.FLOOR) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map[x][y].value = 0;
                }
                x++;
            }
            x = pac.x - 1;
            y = pac.y;
            while (x >= 0 && map[x][y].type == Grid.FLOOR) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map[x][y].value = 0;
                }
                x--;
            }

            x = pac.x;
            y = pac.y + 1;
            while (y < height && map[x][y].type == Grid.FLOOR) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map[x][y].value = 0;
                }
                y++;
            }
            x = pac.x;
            y = pac.y - 1;
            while (y >= 0 && map[x][y].type == Grid.FLOOR) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map[x][y].value = 0;
                }
                y--;
            }
        }
    }

    static void incrementLastSeen() {
        Arrays.stream(map)
                .flatMap(Arrays::stream)
                .forEach(c -> c.turnLastSeen++);
    }

    static void chooseMove(final Pac pac) {
        final Pac enemyPac = enemyNearby(pac);
        if (enemyPac != null && !canKill(enemyPac, pac) && pac.noCooldown()) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(enemyPac);
        } else if (!(turn == 0) && (pac.x == allyPacsLastMove.get(pac.id).x && pac.y == allyPacsLastMove.get(pac.id).y) && !pac.hasUsedCooldownLastTurn()) {
            final Case aCase = goRandomDirection(pac);
            move += "|MOVE " + pac.id + " " + aCase.x + " " + aCase.y;
        } else {
            final Case caseTogo = findNextPellet(pac);
            if (caseTogo.getTaxicabDistance(pac.x, pac.y) > 2 && pac.noCooldown()) {
                move += "|SPEED " + pac.id;
            } else {
                move += "|MOVE " + pac.id + " " + caseTogo.x + " " + caseTogo.y;
            }
        }
    }

    static String switchFormToKill(final Pac enemyPac) {
        switch (enemyPac.typeId) {
            case SCISSORS:
                return ROCK;
            case PAPER:
                return SCISSORS;
            case ROCK:
                return PAPER;
            default:
                return "";
        }
    }

    static boolean canKill(final Pac enemyPac, final Pac pac) {
        return (SCISSORS.equals(enemyPac.typeId) && ROCK.equals(pac.typeId)) || (PAPER.equals(enemyPac.typeId) && SCISSORS.equals(pac.typeId)) || (ROCK.equals(enemyPac.typeId) && PAPER.equals(pac.typeId));
    }

    static Pac enemyNearby(final Pac pac) {
        Pac nearestEnemy = null;
        for (final Pac enemyPac : enemyPacs.values()) {
            if ((enemyPac.x >= pac.x - 1 && enemyPac.x <= pac.x + 1) && (enemyPac.y >= pac.y - 1 && enemyPac.y <= pac.y + 1)) {
                nearestEnemy = enemyPac;
            }
        }
        return nearestEnemy;
    }

    static Case findNextPellet(final Pac pac) {
        return Arrays.stream(map)
                .flatMap(Arrays::stream)
                .filter(p -> p.value > 0)
                .min(Comparator.comparing(p -> p.isWorth(pac.x, pac.y, pac.id)))
                .get();
    }

    static Case goRandomDirection(final Pac pac) {
        final List<Case> cases = new ArrayList<>();
        final List<Case> occupiedCases = Stream.of(allyPacs.values(), enemyPacs.values())
                .flatMap(Collection::stream)
                .map(p -> new Case(p.x, p.y))
                .collect(Collectors.toList());

        if (map[pac.x + 1][pac.y].type == Grid.FLOOR && !occupiedCases.contains(map[pac.x + 1][pac.y])) {
            cases.add(map[pac.x + 1][pac.y]);
        }
        if (map[pac.x - 1][pac.y].type == Grid.FLOOR && !occupiedCases.contains(map[pac.x + 1][pac.y])) {
            cases.add(map[pac.x - 1][pac.y]);
        }
        if (map[pac.x][pac.y + 1].type == Grid.FLOOR && !occupiedCases.contains(map[pac.x + 1][pac.y])) {
            cases.add(map[pac.x][pac.y + 1]);
        }
        if (map[pac.x][pac.y - 1].type == Grid.FLOOR && !occupiedCases.contains(map[pac.x + 1][pac.y])) {
            cases.add(map[pac.x][pac.y - 1]);
        }

        return !cases.isEmpty()
                ? cases.get(new Random().nextInt(cases.size()))
                : map[pac.x][pac.y];
    }

    // UTILS

    enum Grid {
        FLOOR,
        WALL
    }

    // OBJECTS

    static class Pac {
        int id;
        int x;
        int y;
        String typeId;
        int speedTurnsLeft;
        int abilityCooldown;

        public boolean hasUsedCooldownLastTurn() {
            return this.abilityCooldown == 9;
        }

        public boolean noCooldown() {
            return this.abilityCooldown == 0;
        }

        @Override
        public String toString() {
            return "Pac{" +
                    "id=" + this.id +
                    ", x=" + this.x +
                    ", y=" + this.y +
                    ", typeId='" + this.typeId + '\'' +
                    ", speedTurnsLeft=" + this.speedTurnsLeft +
                    ", abilityCooldown=" + this.abilityCooldown +
                    '}';
        }
    }

    static class Case {
        Grid type;
        int x;
        int y;
        int value;
        int turnLastSeen;
        int idClosestPac;

        public Case(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public Case(final Grid type, final int x, final int y, final int value, final int turnLastSeen) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.value = value;
            this.turnLastSeen = turnLastSeen;
        }

        public int isWorth(final int playerX, final int playerY, final int playerId) {
            int weight = this.getTaxicabDistance(playerX, playerY) - (this.value * 2) + this.turnLastSeen;
            if (this.idClosestPac != playerId) {
                weight += 20;
            }
            return weight;
        }

        public int getTaxicabDistance(final int playerX, final int playerY) {
            return Math.abs(playerX - this.x) + Math.abs(playerY - this.y);
        }

        public void setClosestPac() {
            this.idClosestPac = allyPacs.values().stream()
                    .min(Comparator.comparing(p -> this.getTaxicabDistance(p.x, p.y)))
                    .get().id;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            final Case aCase = (Case) o;

            if (this.x != aCase.x) return false;
            return this.y == aCase.y;
        }

        @Override
        public int hashCode() {
            int result = this.x;
            result = 31 * result + this.y;
            return result;
        }

        @Override
        public String toString() {
            return "Case{" +
                    "type=" + this.type +
                    ", x=" + this.x +
                    ", y=" + this.y +
                    ", value=" + this.value +
                    ", turnLastSeen=" + this.turnLastSeen +
                    ", idClosestPac=" + this.idClosestPac +
                    '}';
        }
    }
}
