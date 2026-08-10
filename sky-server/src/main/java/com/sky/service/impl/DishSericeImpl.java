package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishSerice;
import com.sky.vo.DishVO;
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
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        List<DishFlavor> dtoFlavors = dishDTO.getFlavors();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);
        Long dishId = dish.getId();

        if (dtoFlavors != null && dtoFlavors.size() != 0) {
            dtoFlavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
        }
        dishFlavorMapper.insertBatch(dtoFlavors);
    }


    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        List<Dish> dishList = dishMapper.list(dishPageQueryDTO);

        PageInfo<Dish> pageInfo = new PageInfo<>(dishList);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {

        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByIds(ids);

    }

    @Override
    public DishVO selectById(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.selectById(id);
        List<DishFlavor> dishFlavor = dishFlavorMapper.selectByDishId(id);

        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);

        return dishVO;
    }
}
