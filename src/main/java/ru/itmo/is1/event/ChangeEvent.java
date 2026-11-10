package ru.itmo.is1.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangeEvent {
    public enum EntityType { ROUTE, LOCATION, COORDINATES }

    public enum Action { CREATED, UPDATED, DELETED }

    private EntityType entity;
    private Action action;
    private Integer id;
}
