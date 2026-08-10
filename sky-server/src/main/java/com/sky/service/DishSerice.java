package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishSerice {

    /**
     * 新增菜品和对应口味
     *
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    DishVO selectById(Long id);

    void update(DishDTO dishDTO);

    void startOrStop(Integer status, Long id);


    List<Dish> selectByCategoryId(Long categoryId);

}
