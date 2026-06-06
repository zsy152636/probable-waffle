package com.example.quiz.dome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.quiz.dome.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
