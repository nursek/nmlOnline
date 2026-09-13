package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Battle — Impasse mexicaine")
class BattleStandoffTest {

    private Battle battle;
    private final Player joueurA = player(1L, "A");
    private final Player joueurB = player(2L, "B");
    private final Player joueurC = player(3L, "C");

    @BeforeEach
    void setUp() {
        battle = new Battle();
        battle.setRandom(new Random(42));
    }

    private static Player player(long id, String name) {
        Player player = new Player(name);
        player.setId(id);
        return player;
    }

    private static Unit unite(double experience) {
        return new Unit(experience, UnitClass.TIREUR);
    }

    private static Equipment armure(double bonus) {
        return new Equipment("Protection", 100, 0, 0, bonus, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE);
    }

    private static List<List<CombatEntity>> camps(CombatEntity... units) {
        List<List<CombatEntity>> camps = new ArrayList<>();
        for (CombatEntity unit : units) {
            List<CombatEntity> camp = new ArrayList<>();
            camp.add(unit);
            camps.add(camp);
        }
        return camps;
    }

    @Test
    @DisplayName("Chaque camp frappe le suivant : B éliminé par A frappe C dans la même phase")
    void chaqueCampFrappeLeSuivantEnSimultane() {
        Unit bruteA = unite(8);
        Unit larbinB = unite(0);
        Unit larbinC = unite(0);
        List<List<CombatEntity>> camps = camps(bruteA, larbinB, larbinC);

        battle.classicStandoffConfiguration(List.of(joueurA, joueurB, joueurC), camps);

        assertTrue(camps.get(1).isEmpty(), "B est détruit par l'attaque de A");
        assertTrue(camps.get(2).isEmpty(),
                "C est détruit par B dans la même phase : la frappe de B n'est pas annulée par sa mort");
        assertEquals(1, camps.get(0).size(), "A survit");
        assertTrue(bruteA.isInjured(), "A a encaissé la frappe de C");
        assertEquals(joueurA, battle.getWinner(), "Unique survivant : A remporte l'impasse");
    }

    @Test
    @DisplayName("Un camp éliminé en PDF ne frappe plus en ATK (frappes par phase)")
    void campElimineEnPdf_neFrappePasEnAtk() {
        GameCharacter herosA = new GameCharacter("Heros", 0, 50, 0, 200, 0, 0);
        Unit larbinB = unite(0);
        Unit bruteC = unite(8);
        List<List<CombatEntity>> camps = camps(herosA, larbinB, bruteC);

        battle.classicStandoffConfiguration(List.of(joueurA, joueurB, joueurC), camps);

        assertTrue(camps.get(1).isEmpty(), "B tombe sous les 50 PdF du personnage en phase PDF");
        assertEquals(100.0, bruteC.getDefense(),
                "B éliminé en PDF n'a jamais frappé C en ATK");
        assertEquals(100.0, herosA.getDefense(), "C a frappé A en ATK : 200 - 100");
        assertNull(battle.getWinner(), "A et C survivent : aucun vainqueur");
    }

    @Test
    @DisplayName("Plusieurs camps survivants : aucun vainqueur (armure encaissée)")
    void plusieursCampsSurvivants_aucunVainqueur() {
        Unit a = unite(8);
        a.addEquipment(armure(120));
        Unit b = unite(8);
        b.addEquipment(armure(120));
        Unit c = unite(8);
        c.addEquipment(armure(120));
        List<List<CombatEntity>> camps = camps(a, b, c);

        battle.classicStandoffConfiguration(List.of(joueurA, joueurB, joueurC), camps);

        assertEquals(1, camps.get(0).size());
        assertEquals(1, camps.get(1).size());
        assertEquals(1, camps.get(2).size());
        assertEquals(20.0, a.getArmor(), "Chaque camp n'a subi que la frappe de son prédécesseur");
        assertEquals(20.0, b.getArmor());
        assertEquals(20.0, c.getArmor());
        assertNull(battle.getWinner());
    }
}
