package com.example.bookmark.dish;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class DishExcelParser {

    static final String[] HEADERS = {"名称", "标签", "备注", "收藏"};

    private final DataFormatter formatter = new DataFormatter();

    public List<DishRequest> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel 是空的");
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            int nameCol = findColumn(header, "名称", "菜名", "name");
            int tagsCol = findColumn(header, "标签", "tags");
            int noteCol = findColumn(header, "备注", "note");
            int favoriteCol = findColumn(header, "收藏", "favorite");

            boolean hasHeader = nameCol >= 0;
            int startRow = sheet.getFirstRowNum() + (hasHeader ? 1 : 0);
            if (!hasHeader) {
                nameCol = 0;
                tagsCol = 1;
                noteCol = 2;
                favoriteCol = 3;
            }

            List<DishRequest> items = new ArrayList<>();
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = cell(row, nameCol);
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                DishRequest item = new DishRequest();
                item.setName(name);
                item.setTags(cell(row, tagsCol));
                item.setNote(cell(row, noteCol));
                item.setFavorite(parseFavorite(cell(row, favoriteCol)));
                items.add(item);
            }
            return items;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法读取 Excel，请上传 .xlsx 或 .xls");
        }
    }

    public byte[] templateBytes() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("菜单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("黄焖鸡");
            example.createCell(1).setCellValue("快餐,鸡肉");
            example.createCell(2).setCellValue("微辣");
            example.createCell(3).setCellValue("是");
            // autoSizeColumn 对中文偏窄，按 Excel 1/256 字符宽写死
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 32 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成模板失败", ex);
        }
    }

    private int findColumn(Row header, String... aliases) {
        if (header == null) {
            return -1;
        }
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String value = cell(header, i).toLowerCase(Locale.ROOT);
            for (String alias : aliases) {
                if (value.equals(alias.toLowerCase(Locale.ROOT))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String cell(Row row, int col) {
        if (col < 0) {
            return "";
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean parseFavorite(String raw) {
        if (!StringUtils.hasText(raw)) {
            return false;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.equals("1")
                || value.equals("true")
                || value.equals("是")
                || value.equals("y")
                || value.equals("yes");
    }
}
