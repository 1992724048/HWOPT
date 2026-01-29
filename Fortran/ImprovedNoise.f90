module improved_noise_m
  use, intrinsic :: iso_c_binding
  implicit none
  
  type, bind(c) :: noise_state_t
    real(c_double) :: xo, yo, zo
    integer(c_int8_t) :: p(256)
  end type
  
  integer(c_int), parameter :: GRADIENT(3, 0:15) = reshape(&
    [ 1_c_int, 1_c_int, 0_c_int,  -1_c_int, 1_c_int, 0_c_int, &
      1_c_int,-1_c_int, 0_c_int, -1_c_int,-1_c_int, 0_c_int, &
      1_c_int, 0_c_int, 1_c_int,  -1_c_int, 0_c_int, 1_c_int, &
      1_c_int, 0_c_int,-1_c_int, -1_c_int, 0_c_int,-1_c_int, &
      0_c_int, 1_c_int, 1_c_int,  0_c_int,-1_c_int, 1_c_int, &
      0_c_int, 1_c_int,-1_c_int,  0_c_int,-1_c_int,-1_c_int, &
      1_c_int, 1_c_int, 0_c_int,  0_c_int,-1_c_int, 1_c_int, &
     -1_c_int, 1_c_int, 0_c_int,  0_c_int,-1_c_int,-1_c_int ], &
    [3, 16])
  
contains

  pure function smoothstep(x) result(res)
    real(c_double), intent(in) :: x
    real(c_double) :: res
    res = x*x*x*(x*(6.0_c_double*x - 15.0_c_double) + 10.0_c_double)
  end function

  pure function smoothstep_derivative(x) result(res)
    real(c_double), intent(in) :: x
    real(c_double) :: res, x_m1
    x_m1 = x - 1.0_c_double
    res = 30.0_c_double * x * x * x_m1 * x_m1
  end function

  pure function grad_dot(hash, x, y, z) result(res)
    integer(c_int), intent(in) :: hash
    real(c_double), intent(in) :: x, y, z
    real(c_double) :: res
    integer(c_int) :: h
    
    h = iand(hash, 15)
    res = real(GRADIENT(1, h), c_double) * x + &
          real(GRADIENT(2, h), c_double) * y + &
          real(GRADIENT(3, h), c_double) * z
  end function

  pure function perm(state, x) result(res)
    type(noise_state_t), intent(in) :: state
    integer(c_int), intent(in) :: x
    integer(c_int) :: res
    integer(c_int) :: idx
    
    idx = iand(x, 255) + 1
    res = iand(int(state%p(idx), c_int), 255)
  end function

  pure function lerp(alpha, p0, p1) result(res)
    real(c_double), intent(in) :: alpha, p0, p1
    real(c_double) :: res
    res = p0 + alpha * (p1 - p0)
  end function

  pure function lerp2(a1, a2, x00, x10, x01, x11) result(res)
    real(c_double), intent(in) :: a1, a2, x00, x10, x01, x11
    real(c_double) :: res
    res = lerp(a2, lerp(a1, x00, x10), lerp(a1, x01, x11))
  end function

  pure function lerp3(a1, a2, a3, x000, x100, x010, x110, &
                      x001, x101, x011, x111) result(res)
    real(c_double), intent(in) :: a1, a2, a3
    real(c_double), intent(in) :: x000, x100, x010, x110
    real(c_double), intent(in) :: x001, x101, x011, x111
    real(c_double) :: res
    res = lerp(a3, lerp2(a1, a2, x000, x100, x010, x110), &
                   lerp2(a1, a2, x001, x101, x011, x111))
  end function

  function sample_and_lerperm(state, xf, yf, zf, xr, yr, zr, yrOriginal) &
      result(res)
    type(noise_state_t), intent(in) :: state
    integer(c_int), intent(in) :: xf, yf, zf
    real(c_double), intent(in) :: xr, yr, zr, yrOriginal
    real(c_double) :: res
    
    integer(c_int) :: x0, x1, xy00, xy01, xy10, xy11
    real(c_double) :: d000, d100, d010, d110, d001, d101, d011, d111
    real(c_double) :: xAlpha, yAlpha, zAlpha
    
    x0 = perm(state, xf)
    x1 = perm(state, xf + 1)
    xy00 = perm(state, x0 + yf)
    xy01 = perm(state, x0 + yf + 1)
    xy10 = perm(state, x1 + yf)
    xy11 = perm(state, x1 + yf + 1)
    
    d000 = grad_dot(perm(state, xy00 + zf), xr, yr, zr)
    d100 = grad_dot(perm(state, xy10 + zf), xr - 1.0_c_double, yr, zr)
    d010 = grad_dot(perm(state, xy01 + zf), xr, yr - 1.0_c_double, zr)
    d110 = grad_dot(perm(state, xy11 + zf), xr - 1.0_c_double, yr - 1.0_c_double, zr)
    d001 = grad_dot(perm(state, xy00 + zf + 1), xr, yr, zr - 1.0_c_double)
    d101 = grad_dot(perm(state, xy10 + zf + 1), xr - 1.0_c_double, yr, zr - 1.0_c_double)
    d011 = grad_dot(perm(state, xy01 + zf + 1), xr, yr - 1.0_c_double, zr - 1.0_c_double)
    d111 = grad_dot(perm(state, xy11 + zf + 1), xr - 1.0_c_double, yr - 1.0_c_double, zr - 1.0_c_double)
    
    xAlpha = smoothstep(xr)
    yAlpha = smoothstep(yrOriginal)
    zAlpha = smoothstep(zr)
    
    res = lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, &
                d001, d101, d011, d111)
  end function

  function sample_with_derivative(state, xf, yf, zf, xr, yr, zr, deriv) &
      result(res)
    type(noise_state_t), intent(in) :: state
    integer(c_int), intent(in) :: xf, yf, zf
    real(c_double), intent(in) :: xr, yr, zr
    real(c_double), intent(inout) :: deriv(3)
    real(c_double) :: res
    
    integer(c_int) :: x0, x1, xy00, xy01, xy10, xy11
    integer(c_int) :: p000, p100, p010, p110, p001, p101, p011, p111
    integer(c_int) :: g000(3), g100(3), g010(3), g110(3)
    integer(c_int) :: g001(3), g101(3), g011(3), g111(3)
    real(c_double) :: d000, d100, d010, d110, d001, d101, d011, d111
    real(c_double) :: xAlpha, yAlpha, zAlpha
    real(c_double) :: d1x, d1y, d1z, d2x, d2y, d2z
    real(c_double) :: xSD, ySD, zSD, dX, dY, dZ
    
    x0 = perm(state, xf)
    x1 = perm(state, xf + 1)
    xy00 = perm(state, x0 + yf)
    xy01 = perm(state, x0 + yf + 1)
    xy10 = perm(state, x1 + yf)
    xy11 = perm(state, x1 + yf + 1)
    
    p000 = perm(state, xy00 + zf); g000 = GRADIENT(:, iand(p000, 15))
    p100 = perm(state, xy10 + zf); g100 = GRADIENT(:, iand(p100, 15))
    p010 = perm(state, xy01 + zf); g010 = GRADIENT(:, iand(p010, 15))
    p110 = perm(state, xy11 + zf); g110 = GRADIENT(:, iand(p110, 15))
    p001 = perm(state, xy00 + zf + 1); g001 = GRADIENT(:, iand(p001, 15))
    p101 = perm(state, xy10 + zf + 1); g101 = GRADIENT(:, iand(p101, 15))
    p011 = perm(state, xy01 + zf + 1); g011 = GRADIENT(:, iand(p011, 15))
    p111 = perm(state, xy11 + zf + 1); g111 = GRADIENT(:, iand(p111, 15))
    
    d000 = real(g000(1), c_double)*xr + real(g000(2), c_double)*yr + real(g000(3), c_double)*zr
    d100 = real(g100(1), c_double)*(xr-1.0_c_double) + real(g100(2), c_double)*yr + real(g100(3), c_double)*zr
    d010 = real(g010(1), c_double)*xr + real(g010(2), c_double)*(yr-1.0_c_double) + real(g010(3), c_double)*zr
    d110 = real(g110(1), c_double)*(xr-1.0_c_double) + real(g110(2), c_double)*(yr-1.0_c_double) + real(g110(3), c_double)*zr
    d001 = real(g001(1), c_double)*xr + real(g001(2), c_double)*yr + real(g001(3), c_double)*(zr-1.0_c_double)
    d101 = real(g101(1), c_double)*(xr-1.0_c_double) + real(g101(2), c_double)*yr + real(g101(3), c_double)*(zr-1.0_c_double)
    d011 = real(g011(1), c_double)*xr + real(g011(2), c_double)*(yr-1.0_c_double) + real(g011(3), c_double)*(zr-1.0_c_double)
    d111 = real(g111(1), c_double)*(xr-1.0_c_double) + real(g111(2), c_double)*(yr-1.0_c_double) + real(g111(3), c_double)*(zr-1.0_c_double)
    
    xAlpha = smoothstep(xr)
    yAlpha = smoothstep(yr)
    zAlpha = smoothstep(zr)
    
    d1x = lerp3(xAlpha, yAlpha, zAlpha, &
                real(g000(1), c_double), real(g100(1), c_double), &
                real(g010(1), c_double), real(g110(1), c_double), &
                real(g001(1), c_double), real(g101(1), c_double), &
                real(g011(1), c_double), real(g111(1), c_double))
    d1y = lerp3(xAlpha, yAlpha, zAlpha, &
                real(g000(2), c_double), real(g100(2), c_double), &
                real(g010(2), c_double), real(g110(2), c_double), &
                real(g001(2), c_double), real(g101(2), c_double), &
                real(g011(2), c_double), real(g111(2), c_double))
    d1z = lerp3(xAlpha, yAlpha, zAlpha, &
                real(g000(3), c_double), real(g100(3), c_double), &
                real(g010(3), c_double), real(g110(3), c_double), &
                real(g001(3), c_double), real(g101(3), c_double), &
                real(g011(3), c_double), real(g111(3), c_double))
    
    d2x = lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011)
    d2y = lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101)
    d2z = lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110)
    
    xSD = smoothstep_derivative(xr)
    ySD = smoothstep_derivative(yr)
    zSD = smoothstep_derivative(zr)
    
    dX = d1x + xSD * d2x
    dY = d1y + ySD * d2y
    dZ = d1z + zSD * d2z
    
    deriv(1) = deriv(1) + dX
    deriv(2) = deriv(2) + dY
    deriv(3) = deriv(3) + dZ
    
    res = lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, &
                d001, d101, d011, d111)
  end function

  
  !DEC$ ATTRIBUTES DLLEXPORT :: ImprovedNoise_noise_3
  function ImprovedNoise_noise_3(state_ptr, x, y, z) bind(c, name="ImprovedNoise_noise_3")
    type(c_ptr), value :: state_ptr
    real(c_double), value :: x, y, z
    real(c_double) :: ImprovedNoise_noise_3
    type(noise_state_t), pointer :: state
    
    call c_f_pointer(state_ptr, state)
    ImprovedNoise_noise_3 = ImprovedNoise_noise_5(state_ptr, x, y, z, 0.0_c_double, 0.0_c_double)
  end function

  !DEC$ ATTRIBUTES DLLEXPORT :: ImprovedNoise_noise_5
  function ImprovedNoise_noise_5(state_ptr, x, y, z, yScale, yFudge) bind(c, name="ImprovedNoise_noise_5")
    type(c_ptr), value :: state_ptr
    real(c_double), value :: x, y, z, yScale, yFudge
    real(c_double) :: ImprovedNoise_noise_5
    
    type(noise_state_t), pointer :: state
    real(c_double) :: x_adj, y_adj, z_adj, xr, yr, zr, yr_fudge, fudge_limit
    integer(c_int) :: xf, yf, zf
    
    call c_f_pointer(state_ptr, state)
    
    x_adj = x + state%xo
    y_adj = y + state%yo
    z_adj = z + state%zo
    
    xf = floor(x_adj)
    yf = floor(y_adj)
    zf = floor(z_adj)
    xr = x_adj - xf
    yr = y_adj - yf
    zr = z_adj - zf
    
    if (yScale /= 0.0_c_double) then
      if (yFudge >= 0.0_c_double .and. yFudge < yr) then
        fudge_limit = yFudge
      else
        fudge_limit = yr
      end if
      yr_fudge = floor(fudge_limit / yScale + 1.0e-7_c_double) * yScale
    else
      yr_fudge = 0.0_c_double
    end if
    
    ImprovedNoise_noise_5 = sample_and_lerperm(state, xf, yf, zf, xr, yr - yr_fudge, zr, yr)
  end function

  !DEC$ ATTRIBUTES DLLEXPORT :: ImprovedNoise_noise_with_derivative
  function ImprovedNoise_noise_with_derivative(state_ptr, x, y, z, deriv_ptr) bind(c, name="ImprovedNoise_noise_with_derivative")
    type(c_ptr), value :: state_ptr
    real(c_double), value :: x, y, z
    type(c_ptr), value :: deriv_ptr
    real(c_double) :: ImprovedNoise_noise_with_derivative
    
    type(noise_state_t), pointer :: state
    real(c_double), pointer :: deriv(:)
    real(c_double) :: x_adj, y_adj, z_adj, xr, yr, zr
    integer(c_int) :: xf, yf, zf
    
    call c_f_pointer(state_ptr, state)
    call c_f_pointer(deriv_ptr, deriv, [3])
    
    x_adj = x + state%xo
    y_adj = y + state%yo
    z_adj = z + state%zo
    
    xf = floor(x_adj)
    yf = floor(y_adj)
    zf = floor(z_adj)
    xr = x_adj - xf
    yr = y_adj - yf
    zr = z_adj - zf
    
    ImprovedNoise_noise_with_derivative = sample_with_derivative(state, xf, yf, zf, xr, yr, zr, deriv)
  end function

end module