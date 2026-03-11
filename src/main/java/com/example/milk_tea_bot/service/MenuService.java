package com.example.milk_tea_bot.service;

import com.example.milk_tea_bot.model.MenuItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class MenuService {

    private List<MenuItem> menu = new ArrayList<>();

    @PostConstruct
    public void loadMenu() throws Exception {

        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("Menu.csv");

        if (is == null) {
            throw new RuntimeException("Không tìm thấy file Menu.csv trong resources");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        br.readLine(); // bỏ header

        String line;

        while ((line = br.readLine()) != null) {

            String[] p = line.split(",");

            MenuItem item = new MenuItem();

            item.setCategory(p[0]);
            item.setItemId(p[1]);
            item.setName(p[2]);
            item.setDescription(p[3]);
            item.setPriceM(Integer.parseInt(p[4]));
            item.setPriceL(Integer.parseInt(p[5]));
            item.setAvailable(Boolean.parseBoolean(p[6]));

            menu.add(item);
        }

        br.close();
    }

    public List<MenuItem> getMenu() {
        return menu;
    }
    public MenuItem find(String id) {

        for (MenuItem item : menu) {
            if (item.getItemId().equals(id)) {
                return item;
            }
        }

        return null;
    }
}
