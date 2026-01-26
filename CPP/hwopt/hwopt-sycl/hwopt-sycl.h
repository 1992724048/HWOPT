//==============================================================
// Copyright © Intel Corporation
//
// SPDX-License-Identifier: MIT
// =============================================================
#pragma once

#ifdef DPCPP_DLL_EXPORTS
#define DPCPP_DLL_API __declspec(dllexport)
#else
#define DPCPP_DLL_API __declspec(dllimport)
#endif

#include <sycl/sycl.hpp>
#include <vector>
#include <iostream>

using namespace sycl;

typedef std::vector<int> IntVector;

DPCPP_DLL_API void InitializeVector(IntVector& a);
DPCPP_DLL_API void VectorAdd(queue &q, const IntVector &a_vector, const IntVector &b_vector, IntVector &sum_parallel, size_t num_repetitions = 1);
