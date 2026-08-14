package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("套餐相关接口")
@RequestMapping("/admin/setmeal")
@RestController("adminSetmealController")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult<Setmeal>> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询");
        PageResult<Setmeal> pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    @ApiOperation("批量删除套餐")
    public Result deleteByIds(@RequestParam List<Long> ids) {
        log.info("批量删除套餐:{}", ids);
        setmealService.deleteByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result selectById(@PathVariable Long id) {
        log.info("根据id查询套餐:{}", id);
        SetmealVO setmealVO = setmealService.selectById(id);
        return Result.success(setmealVO);
    }

    @PutMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    @ApiOperation("修改套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐");
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    @ApiOperation("套餐起售、停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("套餐起售、停售:{},{}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

}
