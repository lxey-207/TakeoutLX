package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("购物车相关接口")
@Slf4j
@RequestMapping("/user/shoppingCart")
public class ShoppingCartContriller {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result save(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车:{}",shoppingCartDTO);
        shoppingCartService.save(shoppingCartDTO);
        return Result.success();
    }

}
