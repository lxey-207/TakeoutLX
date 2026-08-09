package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishSerice;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishSericeImpl implements DishSerice {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        List<DishFlavor> dtoFlavors = dishDTO.getFlavors();
        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.insert(dish);
        Long dishId = dish.getId();

        if (dtoFlavors != null && dtoFlavors.size() != 0) {
            dtoFlavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
        }
        dishFlavorMapper.insertBatch(dtoFlavors);
    }
}
