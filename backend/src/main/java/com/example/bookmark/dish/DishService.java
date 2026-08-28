package com.example.bookmark.dish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class DishService {

    private final DishRepository dishRepository;
    private final DishExcelParser dishExcelParser;

    public DishService(DishRepository dishRepository, DishExcelParser dishExcelParser) {
        this.dishRepository = dishRepository;
        this.dishExcelParser = dishExcelParser;
    }

    @Transactional(readOnly = true)
    public DishPage list(String q, boolean favoriteOnly, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String keyword = q == null ? "" : q.trim();
        Page<Dish> result = dishRepository.search(keyword, favoriteOnly, pageable);
        return new DishPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Dish get(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new DishNotFoundException(id));
    }

    public Dish create(DishRequest request) {
        Dish dish = new Dish();
        apply(dish, request, true);
        return dishRepository.save(dish);
    }

    public Dish update(Long id, DishRequest request) {
        Dish dish = get(id);
        apply(dish, request, false);
        return dishRepository.save(dish);
    }

    public DishImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new IllegalArgumentException("只支持 .xlsx / .xls");
        }

        List<DishRequest> incoming;
        try {
            incoming = dishExcelParser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new IllegalArgumentException("无法读取 Excel 文件");
        }
        if (incoming.isEmpty()) {
            throw new IllegalArgumentException("Excel 里没有有效的菜名（第一列或「名称」列）");
        }

        Set<String> existingNames = new HashSet<>(dishRepository.findAllNamesLowercase());
        Set<String> seenInBatch = new HashSet<>();
        List<Dish> created = new ArrayList<>();
        int skipped = 0;
        for (DishRequest item : incoming) {
            String key = item.getName().trim().toLowerCase(Locale.ROOT);
            if (!seenInBatch.add(key) || existingNames.contains(key)) {
                skipped++;
                continue;
            }
            created.add(create(item));
        }

        DishImportResult result = new DishImportResult();
        result.setCreated(created.size());
        result.setSkipped(skipped);
        result.setDishes(created);
        return result;
    }

    public byte[] importTemplate() {
        return dishExcelParser.templateBytes();
    }

    public void delete(Long id) {
        if (!dishRepository.existsById(id)) {
            throw new DishNotFoundException(id);
        }
        dishRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Dish random(boolean favoriteOnly) {
        long total = favoriteOnly ? dishRepository.countByFavoriteTrue() : dishRepository.count();
        if (total == 0) {
            throw new DishNotFoundException(favoriteOnly ? "还没有收藏菜，先加几道再抽" : "菜单是空的，先加几道菜");
        }
        int offset = ThreadLocalRandom.current().nextInt((int) Math.min(total, Integer.MAX_VALUE));
        PageRequest pageable = PageRequest.of(offset, 1, Sort.by("id"));
        Page<Dish> page = favoriteOnly
                ? dishRepository.findByFavoriteTrue(pageable)
                : dishRepository.findAll(pageable);
        if (page.isEmpty()) {
            throw new DishNotFoundException(favoriteOnly ? "还没有收藏菜，先加几道再抽" : "菜单是空的，先加几道菜");
        }
        return page.getContent().getFirst();
    }

    private void apply(Dish dish, DishRequest request, boolean creating) {
        dish.setName(request.getName().trim());
        dish.setNote(request.getNote());
        dish.setTags(request.getTags());
        if (request.getFavorite() != null) {
            dish.setFavorite(request.getFavorite());
        } else if (creating) {
            dish.setFavorite(false);
        }

        TasteProfile.Scores inferred = TasteProfile.infer(request.getName(), request.getTags(), request.getNote());
        if (request.getSpice() != null) {
            dish.setSpice(TasteProfile.clamp(request.getSpice()));
        } else if (creating) {
            dish.setSpice(inferred.spice());
        }
        if (request.getSalt() != null) {
            dish.setSalt(TasteProfile.clamp(request.getSalt()));
        } else if (creating) {
            dish.setSalt(inferred.salt());
        }
        if (request.getLight() != null) {
            dish.setLight(TasteProfile.clamp(request.getLight()));
        } else if (creating) {
            dish.setLight(inferred.light());
        }
    }
}
