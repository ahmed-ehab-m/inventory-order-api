package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
/// no @RequiredArgsConstructor because we want to send user repo to base service
public class UserService extends BaseService<UserEntity,Long> {
    private  final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private  final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        super(userRepo);
        this.userRepo=userRepo;
        this.userMapper=userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }
    /////////////////////////
    /// READ METHODS
    /// GET BY EMAIL
    public UserResponseDto findByEmail(String email)
    {
        UserEntity userEntity=userRepo.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User","email",email));
        return  userMapper.mapToDto(userEntity);
    }
    /////GET ALL
    /// smart method for pagination
    /// take filter => smart object contains page number , size ,sort type
    /// return pageResponse we created in core folder
    public PageResponse<UserResponseDto> getUsersPage(UserFilterRequest filter)
    {
        ///// 1=> take user input => "ASC" OR "DESC" from headers
        Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());

        ////// 2=> Pageable => take all user input
        ///// will be translated to SQL
        Pageable pageable= PageRequest.of(filter.getPage(),filter.getSize(),sort);
        ///// holds data + meta data about it
        Specification<UserEntity> spec=UserSpecification.buildFilter(filter);
        //// 3=> call Repo
        Page<UserEntity> userEntityPage=userRepo.findAll(spec,pageable);
        ///// map Entities to Dtos
        List<UserResponseDto> dtos=userMapper.mapToDtoList(userEntityPage.getContent());
        return  PageResponse.from(userEntityPage,dtos);
    }
    //// GET DELETED USERS
    public PageResponse<UserResponseDto> getDeletedUsersPage(UserFilterRequest filter)
    {
        ///// 1=> take user input => "ASC" OR "DESC" from headers
        Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());

        ////// 2=> Pageable => take all user input
        ///// will be translated to SQL
        Pageable pageable= PageRequest.of(filter.getPage(),filter.getSize(),sort);
        ///// holds data + meta data about it
        Specification<UserEntity> spec=UserSpecification.buildFilter(filter);
        //// 3=> call Repo
        Page<UserEntity> userEntityPage=userRepo.findAllDeletedUsers(spec,pageable);
        ///// map Entities to Dtos
        List<UserResponseDto> dtos=userMapper.mapToDtoList(userEntityPage.getContent());
        return  PageResponse.from(userEntityPage,dtos);
    }
    ////////////////////////////////////
    /// WRITE METHODS
    //// REGISTER USER
    @Transactional
    public AuthResponseDto register(UserRequestDto user)
    {
        //// 1=>  check email first
        if(userRepo.existsByEmail(user.getEmail()))
        {
            throw new DuplicateRecordException("User", "email", user.getEmail());
        }
        //// 2=> map to Dto
        UserEntity userEntity=userMapper.mapToEntity(user);

        //// 3=> encoding password
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));

        //// 4=> Set Role
        userEntity.setRole(UserRole.USER);

        //// 5=> save in DB
        UserEntity savedUser= userRepo.save(userEntity);

        /// 6=> Auto Login => Generate token for new user
        /// to go home page direct not login page to take token
        String token=jwtService.generateToken(new UserPrincipal(savedUser));

        /// 7=> map to dto
        UserResponseDto userResponseDto=userMapper.mapToDto(savedUser);

        /// 8=> return token + user data
        return new AuthResponseDto(token,userResponseDto);

    }

    //// LOGIN USER
    public AuthResponseDto login(UserRequestDto user)
    {
        //// 1=>  check email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(),user.getPassword())
        );
        //// 2=> get user from db
        UserEntity userEntity=userRepo.findByEmail(user.getEmail()).orElseThrow();

        //// 3=> generate jwt token
        String token =jwtService.generateToken(new UserPrincipal(userEntity));

        //// 4=> Map Entity to DTO
        UserResponseDto userResponseDto=userMapper.mapToDto(userEntity);

        /// return user data +token
        return new AuthResponseDto(token,userResponseDto);
    }

    //// UPDATE USER
    @Transactional
    public UserResponseDto updateUser(Long id , UserRequestDto updateRequest)
    {
        /// 1=> get old user from db // Base Service
        UserEntity existingUser=findById(id);

        /// 2=> check if user change old email with new email of another user
        if(!existingUser.getEmail().equals(updateRequest.getEmail())
        && userRepo.existsByEmail(updateRequest.getEmail()))
        {
            throw new DuplicateRecordException("User", "email", updateRequest.getEmail());
        }
        /// 3=> update data
        existingUser.setName(updateRequest.getName());
        existingUser.setEmail(updateRequest.getEmail());
        if (updateRequest.getLocation() != null && !updateRequest.getLocation().isBlank()) {
            existingUser.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPhone() != null && !updateRequest.getPhone().isBlank()) {
            existingUser.setPhone(updateRequest.getPhone());
        }
        if(updateRequest.getPassword() !=null && !updateRequest.getPassword().isBlank())
        {
            existingUser.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        return  userMapper.mapToDto(userRepo.save(existingUser));
    }
    ////////////////////
    /// SOFT DELETE METHOD
    @Transactional
    public void softDeleteUser(Long id) {
        delete(id);
    }
    /// HARD DELETE METHODS
    @Transactional
    public  void hardDeleteMethod(Long id)
    {
        if(!userRepo.existsById(id))
        {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepo.hardDeleteUser(id);
    }
    //// RESTORE USER
    @Transactional
    public  void restoreUser(Long id)
    {
        userRepo.restoreUser(id);
    }
}
