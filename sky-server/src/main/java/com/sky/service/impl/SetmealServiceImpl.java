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
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        Long setmealId = setmeal.getId();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(sd -> sd.setSetmealId(setmealId));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public PageResult<Setmeal> page(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        List<Setmeal> setmealList = setmealMapper.list(setmealPageQueryDTO);

        PageInfo<Setmeal> pageInfo = new PageInfo<>(setmealList);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());

    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {

        if (ids != null && ids.size() > 0) {
            setmealMapper.deleteByIds(ids);
            setmealDishMapper.deleteByIds(ids);
        }
    }

    @Override
    public SetmealVO selectById(Long id) {

        SetmealVO setmealVO = new SetmealVO();

        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectById(id);

        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }


    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        List<Long> setmealIds = new ArrayList<>();

        for (SetmealDish setmealDish : setmealDishes) {
            setmealIds.add(setmeal.getId());
        }

        BeanUtils.copyProperties(setmealDTO, setmeal);

        setmealMapper.update(setmeal);
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishMapper.deleteByIds(setmealIds);
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public void startOrStop(Integer status, Long id) {

        Setmeal setmeal = setmealMapper.selectById(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);

    }
}
