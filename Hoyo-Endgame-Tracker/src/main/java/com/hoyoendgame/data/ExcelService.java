package com.hoyoendgame.data;

import com.hoyoendgame.model.Element;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TeamMember;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel (.xlsx) import / export of the roster and the teams.
 * Sheet "Personnages": Nom | Élément | Niveau | Doublons | Arme | Raffinement
 * Sheet "Equipes": Équipe | Usage | Emplacement | Personnage | Élément | Niveau | Doublons | Arme | Raffinement
 */
public final class ExcelService {
    public static final String SHEET_CHARACTERS = "Personnages";
    public static final String SHEET_TEAMS = "Equipes";

    public record Imported(List<GenshinCharacter> characters, List<Team> teams) {
    }

    private ExcelService() {
    }

    public static void export(File target, List<GenshinCharacter> characters, List<Team> teams) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);

            Sheet sc = wb.createSheet(SHEET_CHARACTERS);
            header(sc, head, "Nom", "Élément", "Niveau", "Doublons", "Arme", "Raffinement");
            int r = 1;
            for (GenshinCharacter c : characters) {
                Row row = sc.createRow(r++);
                row.createCell(0).setCellValue(c.name);
                row.createCell(1).setCellValue(c.element == null ? "" : c.element.label());
                row.createCell(2).setCellValue(c.level);
                row.createCell(3).setCellValue(c.constellation);
                row.createCell(4).setCellValue(c.weapon == null ? "" : c.weapon);
                row.createCell(5).setCellValue(c.refinement);
            }
            for (int i = 0; i < 6; i++) sc.autoSizeColumn(i);

            Sheet st = wb.createSheet(SHEET_TEAMS);
            header(st, head, "Équipe", "Usage", "Emplacement", "Personnage", "Élément", "Niveau", "Doublons", "Arme", "Raffinement");
            r = 1;
            for (Team t : teams) {
                int slot = 1;
                for (TeamMember m : t.members) {
                    Row row = st.createRow(r++);
                    row.createCell(0).setCellValue(t.name);
                    row.createCell(1).setCellValue(t.usage);
                    row.createCell(2).setCellValue(slot++);
                    row.createCell(3).setCellValue(m.name);
                    row.createCell(4).setCellValue(m.element == null ? "" : m.element.label());
                    row.createCell(5).setCellValue(m.level);
                    row.createCell(6).setCellValue(m.constellation);
                    row.createCell(7).setCellValue(m.weapon == null ? "" : m.weapon);
                    row.createCell(8).setCellValue(m.refinement);
                }
            }
            for (int i = 0; i < 9; i++) st.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(target)) {
                wb.write(out);
            }
        }
    }

    public static Imported importFrom(File source) throws IOException {
        DataFormatter fmt = new DataFormatter(Locale.FRANCE);
        List<GenshinCharacter> characters = new ArrayList<>();
        Map<String, Team> teams = new LinkedHashMap<>();

        try (Workbook wb = WorkbookFactory.create(source, null, true)) {
            Sheet sc = wb.getSheet(SHEET_CHARACTERS);
            if (sc != null) {
                for (int i = 1; i <= sc.getLastRowNum(); i++) {
                    Row row = sc.getRow(i);
                    if (row == null) continue;
                    String name = str(fmt, row, 0);
                    if (name.isBlank()) continue;
                    GenshinCharacter c = new GenshinCharacter();
                    c.name = name;
                    Element e = Element.parse(str(fmt, row, 1));
                    c.element = e == null ? Element.GEO : e;
                    c.level = num(fmt, row, 2, 90);
                    c.constellation = num(fmt, row, 3, 0);
                    c.weapon = str(fmt, row, 4);
                    c.refinement = num(fmt, row, 5, 1);
                    c.normalize();
                    characters.add(c);
                }
            }

            Sheet st = wb.getSheet(SHEET_TEAMS);
            if (st != null) {
                for (int i = 1; i <= st.getLastRowNum(); i++) {
                    Row row = st.getRow(i);
                    if (row == null) continue;
                    String teamName = str(fmt, row, 0);
                    String charName = str(fmt, row, 3);
                    if (teamName.isBlank() || charName.isBlank()) continue;
                    Team t = teams.computeIfAbsent(teamName, n -> {
                        Team nt = new Team();
                        nt.name = n;
                        nt.usage = str(fmt, row, 1).isBlank() ? "Libre" : str(fmt, row, 1);
                        return nt;
                    });
                    TeamMember m = new TeamMember();
                    m.name = charName;
                    Element e = Element.parse(str(fmt, row, 4));
                    m.element = e == null ? Element.GEO : e;
                    m.level = Math.max(1, Math.min(90, num(fmt, row, 5, 90)));
                    m.constellation = Math.max(0, Math.min(6, num(fmt, row, 6, 0)));
                    m.weapon = str(fmt, row, 7);
                    m.refinement = Math.max(1, Math.min(5, num(fmt, row, 8, 1)));
                    for (GenshinCharacter c : characters) {
                        if (c.name.equalsIgnoreCase(charName)) {
                            m.characterId = c.id;
                            break;
                        }
                    }
                    if (t.members.size() < 4) t.members.add(m);
                }
            }
        }
        return new Imported(characters, new ArrayList<>(teams.values()));
    }

    private static void header(Sheet sheet, CellStyle style, String... titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(style);
        }
    }

    private static String str(DataFormatter fmt, Row row, int col) {
        var cell = row.getCell(col);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }

    private static int num(DataFormatter fmt, Row row, int col, int def) {
        String s = str(fmt, row, col).replace(',', '.');
        if (s.isEmpty()) return def;
        try {
            return (int) Math.round(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
