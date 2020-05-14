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

    static List<Case> superPellets = new ArrayList<>();

    static Map<Coord, Case> map = new HashMap<>();

    static int turn;

    public static void main(final String[] args) {
        final Scanner in = new Scanner(System.in);
        width = in.nextInt();
        height = in.nextInt();

        if (in.hasNextLine()) {
            in.nextLine();
        }

        for (int i = 0; i < height; i++) {
            final String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for (int j = 0; j < row.length(); j++) {
                if (' ' == row.charAt(j)) {
                    map.put(new Coord(j, i), new Case(j, i, 1, 0));
                }
            }
        }
        setAdjacentCases();
        // game loop
        for (turn = 0; turn < 200; turn++) {
            final List<Case> superPelletsThisTurn = new ArrayList<>();
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
                final int x = in.nextInt();
                final int y = in.nextInt();
                pac.coord = new Coord(x, y);
                pac.typeId = in.next();
                pac.speedTurnsLeft = in.nextInt();
                pac.abilityCooldown = in.nextInt();
                if (mine && !"DEAD".equals(pac.typeId)) {
                    allyPacs.put(pacId, pac);
                } else if (!"DEAD".equals(pac.typeId)) {
                    enemyPacs.put(pacId, pac);
                }
                map.get(pac.coord).value = 0;
            }
            visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                final int x = in.nextInt();
                final int y = in.nextInt();
                final int value = in.nextInt(); // amount of points this pellet is worth
                final Coord coord = new Coord(x, y);
                final Case aCase = new Case(x, y, value, turn);
                map.get(coord).turnLastSeen = 0;
                visiblePellets.add(aCase);
                if (value == 10) {
                    map.get(coord).value = 10;
                    superPelletsThisTurn.add(map.get(coord));
                }

            }
            removeNonExistentPelletsInSight(visiblePellets);
            removeNonExistentSuperPellets(superPelletsThisTurn);
            findClosestPac();
            move = "";
            for (final Pac pac : allyPacs.values()) {
                chooseMove(pac, superPelletsThisTurn);
            }
            System.out.println(move.substring(1));
            allyPacsLastMove = allyPacs;
        }
    }

    // ALGORITHMS

    static void setAdjacentCases() {
        map.values().forEach(c -> {
            int x = c.coord.x + 1 < width ? c.coord.x + 1 : 0;
            int y = c.coord.y;
            Coord coord = new Coord(x, y);
            if (map.containsKey(coord)) {
                c.adjacentCases.add(map.get(coord));
            }

            x = c.coord.x - 1 >= 0 ? c.coord.x - 1 : width - 1;
            coord = new Coord(x, y);
            if (map.containsKey(coord)) {
                c.adjacentCases.add(map.get(coord));
            }

            x = c.coord.x;
            y = c.coord.y + 1 < height ? c.coord.y + 1 : 0;
            coord = new Coord(x, y);
            if (map.containsKey(coord)) {
                c.adjacentCases.add(map.get(coord));
            }

            y = c.coord.y - 1 >= 0 ? c.coord.y - 1 : height - 1;
            coord = new Coord(x, y);
            if (map.containsKey(coord)) {
                c.adjacentCases.add(map.get(coord));
            }
        });
    }

    static void removeNonExistentSuperPellets(final List<Case> superPelletsThisTurn) {
        if (turn != 0) {
            superPellets.removeAll(superPelletsThisTurn);
            superPellets.forEach(c -> map.get(c.coord).value = 0);
        }
        superPellets = superPelletsThisTurn;
    }

    static void findClosestPac() {
        map.values().forEach(Case::setClosestPac);
    }

    static Coord getMostValuableCase(final Pac pac) {
        final List<Case[]> paths = new ArrayList<>();

        final Case[] path = new Case[5];
        for (final Case case0 : map.get(pac.coord).adjacentCases) {
            path[0] = case0;
            for (final Case case1 : case0.adjacentCases) {
                path[1] = case1;
                for (final Case case2 : case1.adjacentCases) {
                    path[2] = case2;
                    for (final Case case3 : case2.adjacentCases) {
                        path[3] = case3;
                        for (final Case case4 : case3.adjacentCases) {
                            path[4] = case4;
                            paths.add(path.clone());
                        }
                    }
                }
            }
        }
        int max = Integer.MIN_VALUE;
        Coord coordToGo = null;
        for (final Case[] caseTab : paths) {
            final int maxTemp = Arrays.stream(caseTab).mapToInt(c -> c.isWorth(pac)).sum();
            if (maxTemp > max) {
                coordToGo = caseTab[1].coord;
                max = maxTemp;
            }
        }
        return max <= 0 ? findNextPellet(pac).coord : coordToGo;
    }

    static Optional<Case> getNearestSuperPellet(final Pac pac, final List<Case> superPelletsThisTurn) {
        return superPelletsThisTurn.stream()
                .filter(c -> c.idClosestPac == pac.id)
                .min(Comparator.comparing(c -> c.getTaxicabDistance(pac.coord)));
    }

    static void removeNonExistentPelletsInSight(final Set<Case> visiblePellets) {
        for (final Pac pac : allyPacs.values()) {
            int x = pac.coord.x + 1;
            int y = pac.coord.y;
            while (x < width && map.get(new Coord(x, y)) != null) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map.get(new Coord(x, y)).value = 0;
                }
                x++;
            }
            x = pac.coord.x - 1;
            y = pac.coord.y;
            while (x >= 0 && map.get(new Coord(x, y)) != null) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map.get(new Coord(x, y)).value = 0;
                }
                x--;
            }

            x = pac.coord.x;
            y = pac.coord.y + 1;
            while (y < height && map.get(new Coord(x, y)) != null) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map.get(new Coord(x, y)).value = 0;
                }
                y++;
            }
            x = pac.coord.x;
            y = pac.coord.y - 1;
            while (y >= 0 && map.get(new Coord(x, y)) != null) {
                if (!visiblePellets.contains(new Case(x, y))) {
                    map.get(new Coord(x, y)).value = 0;
                }
                y--;
            }
        }
    }

    static void incrementLastSeen() {
        map.values().forEach(c -> c.turnLastSeen++);
    }

    static void chooseMove(final Pac pac, final List<Case> superPelletsThisTurn) {
        final Pac enemyPac = enemyNearby(pac);
        if (enemyPac != null && !canKill(enemyPac, pac) && pac.noCooldown()) {
            move += "|SWITCH " + pac.id + " " + switchFormToKill(enemyPac);
        } else if (turn != 0 && (pac.coord.equals(allyPacsLastMove.get(pac.id).coord) && !pac.hasUsedCooldownLastTurn())) {
            final Case aCase = goRandomDirection(pac);
            move += "|MOVE " + pac.id + " " + aCase.coord.x + " " + aCase.coord.y;
        } else {
            final Case caseTogo = getNearestSuperPellet(pac, superPelletsThisTurn)
                    .orElseGet(() -> map.get(getMostValuableCase(pac)));
            if (pac.noCooldown()) {
                move += "|SPEED " + pac.id;
            } else {
                move += "|MOVE " + pac.id + " " + caseTogo.coord.x + " " + caseTogo.coord.y;
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
        return (SCISSORS.equals(enemyPac.typeId) && ROCK.equals(pac.typeId))
                || (PAPER.equals(enemyPac.typeId) && SCISSORS.equals(pac.typeId))
                || (ROCK.equals(enemyPac.typeId) && PAPER.equals(pac.typeId));
    }

    static Pac enemyNearby(final Pac pac) {
        Pac nearestEnemy = null;
        for (final Pac enemyPac : enemyPacs.values()) {
            if ((enemyPac.coord.x >= pac.coord.x - 1
                    && enemyPac.coord.x <= pac.coord.x + 1)
                    && (enemyPac.coord.y >= pac.coord.y - 1
                    && enemyPac.coord.y <= pac.coord.y + 1)) {
                nearestEnemy = enemyPac;
            }
        }
        return nearestEnemy;
    }

    static Case findNextPellet(final Pac pac) {
        return map.values().stream()
                .filter(p -> p.value > 0)
                .max(Comparator.comparing(p -> p.isWorth(pac)))
                .get();
    }

    static Case goRandomDirection(final Pac pac) {

        final List<Case> occupiedCases = Stream.of(allyPacs.values(), enemyPacs.values())
                .flatMap(Collection::stream)
                .map(p -> new Case(p.coord))
                .collect(Collectors.toList());

        final List<Case> cases = map.get(pac.coord).adjacentCases.stream()
                .filter(c -> !occupiedCases.contains(c))
                .collect(Collectors.toList());

        return cases
                .get(new Random().nextInt(cases.size()));
    }

    // OBJECTS

    static class Pac {
        int id;
        Coord coord;
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
                    ", coord=" + this.coord +
                    ", typeId='" + this.typeId + '\'' +
                    ", speedTurnsLeft=" + this.speedTurnsLeft +
                    ", abilityCooldown=" + this.abilityCooldown +
                    '}';
        }
    }

    static class Coord {
        int x;
        int y;

        public Coord(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            final Coord coord = (Coord) o;
            return this.x == coord.x &&
                    this.y == coord.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.x, this.y);
        }

        @Override
        public String toString() {
            return "Coord{" +
                    "x=" + this.x +
                    ", y=" + this.y +
                    '}';
        }
    }

    static class Case {
        Coord coord;
        int value;
        int turnLastSeen;
        int idClosestPac;

        List<Case> adjacentCases = new ArrayList<>();

        public Case(final int x, final int y) {
            this.coord = new Coord(x, y);
        }

        public Case(final Coord coord) {
            this.coord = coord;
        }

        public Case(final int x, final int y, final int value, final int turnLastSeen) {
            this(x, y);
            this.value = value;
            this.turnLastSeen = turnLastSeen;
        }

        public int isWorth(final Pac pac) {
            int weight = (this.value * 2);
            if (this.value > 0) {
                weight += 200 - this.turnLastSeen;
            }
            if (this.idClosestPac != pac.id) {
                weight -= 20;
            }
            return weight;
        }

        public int getTaxicabDistance(final Coord playerCoord) {
            return Math.abs(playerCoord.x - this.coord.x) + Math.abs(playerCoord.y - this.coord.y);
        }

        public void setClosestPac() {
            this.idClosestPac = Stream.of(allyPacs.values(), enemyPacs.values())
                    .flatMap(Collection::stream)
                    .min(Comparator.comparing(p -> this.getTaxicabDistance(p.coord)))
                    .get().id;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            final Case aCase = (Case) o;
            return Objects.equals(this.coord, aCase.coord);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.coord);
        }

        @Override
        public String toString() {
            final List<Coord> adjacent = this.adjacentCases.stream()
                    .map(c -> c.coord)
                    .collect(Collectors.toList());
            return "Case{" +
                    "coord=" + this.coord +
                    ", value=" + this.value +
                    ", turnLastSeen=" + this.turnLastSeen +
                    ", idClosestPac=" + this.idClosestPac +
                    ", adjacentCases=" + adjacent +
                    '}';
        }
    }
}
