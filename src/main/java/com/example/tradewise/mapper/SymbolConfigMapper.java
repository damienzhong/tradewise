package com.example.tradewise.mapper;

import com.example.tradewise.entity.SymbolConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SymbolConfigMapper {

    @Select("SELECT * FROM symbol_config WHERE enabled = 1")
    List<SymbolConfig> findAllEnabled();

    @Select("SELECT * FROM symbol_config")
    List<SymbolConfig> findAll();

    @Insert("INSERT INTO symbol_config (symbol, enabled) VALUES (#{symbol}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SymbolConfig symbolConfig);

    @Update("UPDATE symbol_config SET symbol = #{symbol}, enabled = #{enabled} WHERE id = #{id}")
    int update(SymbolConfig symbolConfig);

    @Delete("DELETE FROM symbol_config WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM symbol_config WHERE symbol = #{symbol}")
    SymbolConfig findBySymbol(String symbol);
}
