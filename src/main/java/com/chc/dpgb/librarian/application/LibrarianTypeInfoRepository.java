package com.chc.dpgb.librarian.application;

import java.util.List;

import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

public interface LibrarianTypeInfoRepository {

    List<LibrarianTypeInfo> findAll();
}
