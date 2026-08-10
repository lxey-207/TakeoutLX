package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.insert(setmeal);
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public PageResult<Setmeal> page(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        List<Setmeal> setmealList = setmealMapper.list(setmealPageQueryDTO);

        PageInfo<Setmeal> pageInfo = new PageInfo<>(setmealList);

        return new PageResult<>(pageInfo.getTotal(),pageInfo.getList());

    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {

        if (ids != null && ids.size() > 0) {
            setmealMapper.deleteByIds(ids);
            setmealDishMapper.deleteByIds(ids);
        }

    }
}
