package org.jay.mybatis.generator.plugin.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ListResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    long total = 0;
    
    List<T> data;
    
    public List<T> getData() {
        if (data == null) {
            return data = new ArrayList<T>(0);
        }
        return data;
    }
    
}