package com.example.iknow.block;

/**
 * 无限源方块实体的通用服务端 tick，用于方块 getTicker 统一调用。
 */
public interface IInfiniteSource {
    void infiniteServerTick();
}
