package com.sky.controller.user;

import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("套餐相关接口")
@RequestMapping("/user/setmeal")
@RestController("userSetmealController")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/list")
    @Cacheable(cacheNames = "setmealCache",key = "#categoryId")
    @ApiOperation("根据分类id查询套餐")
    public Result list(Long categoryId) {
        log.info("根据分类id查询套餐:{}", categoryId);
        List<Setmeal> setmealList = setmealService.selectByCategoryId(categoryId);
        return Result.success(setmealList);
    }

    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }

}
